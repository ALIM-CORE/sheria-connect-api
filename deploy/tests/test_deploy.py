"""Exercise the deployment helper without connecting to Docker, GHCR or the VPS."""
import json
import os
import pathlib
import shutil
import subprocess
import tempfile
import unittest


SHA = "a" * 40
DIGEST = "b" * 64
SOURCE = pathlib.Path(__file__).resolve().parents[1] / "ops" / "deploy.sh"

MOCK = r"""#!/usr/bin/env bash
set -eu
name=$(basename "$0")
printf '%s %s\n' "$name" "$*" >> "$MOCK_ROOT/commands.log"
case "$name" in
  sleep) exit 0 ;;
  curl)
    if [[ "${MOCK_FAILURE:-}" == readiness ]] ||
       { [[ "${MOCK_FAILURE:-}" == public ]] && [[ "$*" == *https://* ]]; }; then exit 22; fi
    while [[ $# -gt 0 ]]; do
      if [[ "$1" == -o ]]; then output=$2; break; fi
      shift
    done
    printf '{"status":"UP","service":"%s","revision":"%s"}' "$MOCK_MARKER" "$MOCK_SHA" > "$output"
    ;;
  docker)
    case "$*" in
      pull*) [[ "${MOCK_FAILURE:-}" != pull ]] ;;
      *'org.opencontainers.image.revision'*)
        if [[ "${MOCK_FAILURE:-}" == label ]]; then echo wrong; else echo "$MOCK_SHA"; fi ;;
      *'.Config.Image'*) echo "ghcr.io/alim-core/$MOCK_REPOSITORY:previous" ;;
      *'{{.Id}}'*|*'{{.Image}}'*) echo image-id ;;
      *'ps -a -q'*|*'ps -q'*) [[ "${MOCK_BOOTSTRAP:-}" != true ]] && echo container-id || true ;;
      *'pg_dump'*) [[ "${MOCK_FAILURE:-}" != backup ]] && printf database-dump ;;
      *'pg_restore'*) cat >/dev/null ;;
      *'up -d'*) [[ "${MOCK_FAILURE:-}" != up ]] ;;
      *) exit 0 ;;
    esac
    ;;
esac
"""


@unittest.skipUnless(os.name == "posix", "Run this Bash deployment suite on Linux or WSL")
class DeploymentTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = pathlib.Path(self.temp.name)
        (self.root / "ops").mkdir()
        shutil.copyfile(SOURCE, self.root / "ops" / "deploy.sh")
        (self.root / "bin").mkdir()
        for command in ("docker", "curl", "sleep"):
            executable = self.root / "bin" / command
            executable.write_text(MOCK)
            executable.chmod(0o755)
        (self.root / ".env").write_text("SECRET=do-not-print\n")
        (self.root / "compose.production.yaml").write_text("services: {}\n")
        self.initial = "BACKEND_IMAGE=old-backend\nADMIN_IMAGE=old-admin\nCITIZEN_IMAGE=old-citizen\nWEBSITE_IMAGE=old-website\n"
        (self.root / ".releases.env").write_text(self.initial)

    def run_deploy(self, service="frontend", failure="", bootstrap=False):
        repository = "sheria-connect-backend" if service == "backend" else "sheria-connect-frontend"
        self.candidate = f"ghcr.io/alim-core/{repository}@sha256:{DIGEST}"
        environment = {
            **os.environ,
            "PATH": str(self.root / "bin") + os.pathsep + os.environ["PATH"],
            "MOCK_ROOT": str(self.root),
            "MOCK_FAILURE": failure,
            "MOCK_MARKER": "sheria-connect-api" if service == "backend" else "sheria-connect-admin",
            "MOCK_REPOSITORY": repository,
            "MOCK_SHA": SHA,
            "MOCK_BOOTSTRAP": str(bootstrap).lower(),
        }
        command = ["bash", str(self.root / "ops" / "deploy.sh"), service, self.candidate, SHA]
        if bootstrap:
            command.append("--bootstrap")
        result = subprocess.run(command, env=environment, capture_output=True, text=True, timeout=30)
        self.assertNotIn("do-not-print", result.stdout + result.stderr)
        return result

    def releases(self):
        return (self.root / ".releases.env").read_text()

    def commands(self):
        return (self.root / "commands.log").read_text()

    def test_success_pins_only_its_own_service_and_checks_public_revision(self):
        result = self.run_deploy()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("ADMIN_IMAGE=" + self.candidate, self.releases())
        self.assertIn("BACKEND_IMAGE=old-backend", self.releases())
        self.assertIn("https://admin.sheriaconnect.co.tz/release.json", self.commands())
        self.assertIn("--no-deps --no-build --force-recreate --pull never frontend", self.commands())
        self.assertTrue((self.root / "deployments.log").exists())

    def test_failed_pull_cannot_mutate_state_or_recreate_container(self):
        self.assertNotEqual(0, self.run_deploy(failure="pull").returncode)
        self.assertEqual(self.initial, self.releases())
        self.assertNotIn("up -d", self.commands())

    def test_wrong_image_revision_is_rejected_before_mutation(self):
        self.assertNotEqual(0, self.run_deploy(failure="label").returncode)
        self.assertEqual(self.initial, self.releases())
        self.assertNotIn("up -d", self.commands())

    def test_failed_database_backup_blocks_backend_deployment(self):
        self.assertNotEqual(0, self.run_deploy(service="backend", failure="backup").returncode)
        self.assertEqual(self.initial, self.releases())
        self.assertNotIn("up -d", self.commands())

    def test_unhealthy_frontend_restores_baseline_but_returns_failure(self):
        self.assertNotEqual(0, self.run_deploy(failure="readiness").returncode)
        self.assertIn("ADMIN_IMAGE=ghcr.io/alim-core/sheria-connect-frontend:previous", self.releases())
        self.assertEqual(2, self.commands().count("up -d"))

    def test_wrong_public_routing_rolls_back_frontend(self):
        self.assertNotEqual(0, self.run_deploy(failure="public").returncode)
        self.assertIn(":previous", self.releases())

    def test_backend_failure_keeps_backup_and_never_automatically_rolls_back(self):
        self.assertNotEqual(0, self.run_deploy(service="backend", failure="readiness").returncode)
        self.assertIn("BACKEND_IMAGE=" + self.candidate, self.releases())
        self.assertEqual(1, self.commands().count("up -d"))
        self.assertEqual(1, len(list((self.root / "backups").glob("*.dump"))))

    def test_bootstrap_checks_local_readiness_without_requiring_new_dns(self):
        result = self.run_deploy(bootstrap=True)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertNotIn("https://", self.commands())


if __name__ == "__main__":
    unittest.main()
