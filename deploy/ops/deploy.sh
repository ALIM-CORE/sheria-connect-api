#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

fail() { echo "ERROR: $*" >&2; exit 1; }
[[ $# -ge 3 && $# -le 4 ]] || fail "Usage: deploy.sh SERVICE IMAGE_DIGEST REVISION [--bootstrap]"
service=$1
image=$2
revision=$3
bootstrap=${4:-}
[[ -z "$bootstrap" || "$bootstrap" == --bootstrap ]] || fail "Invalid bootstrap flag"
[[ "$revision" =~ ^[a-f0-9]{40}$ ]] || fail "Expected a full Git commit SHA"
case "$service" in
  backend) key=BACKEND_IMAGE; repository=sheria-connect-backend; marker=sheria-connect-api; port=6001; public=https://api.sheriaconnect.co.tz ;;
  frontend) key=ADMIN_IMAGE; repository=sheria-connect-frontend; marker=sheria-connect-admin; port=4101; public=https://admin.sheriaconnect.co.tz ;;
  citizen) key=CITIZEN_IMAGE; repository=sheria-connect-app; marker=sheria-connect-app; port=4001; public=https://app.sheriaconnect.co.tz ;;
  website) key=WEBSITE_IMAGE; repository=sheria-connect-website; marker=sheria-connect-website; port=4201; public=https://sheriaconnect.co.tz ;;
  *) fail "Unknown service" ;;
esac
[[ "$image" =~ ^ghcr.io/alim-core/$repository@sha256:[a-f0-9]{64}$ ]] || fail "Expected the service's GHCR digest"
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
for command in docker curl python3 flock; do command -v "$command" >/dev/null || fail "Missing $command"; done
[[ -f .env && -f .releases.env && -f compose.production.yaml ]] || fail "Production configuration has not been installed"

# All four repositories share this lock. Never cancel a deployment mid-migration.
exec 9>.production-deploy.lock
flock -w 600 9 || fail "Timed out waiting for another production deployment"
compose=(docker compose --env-file .env --env-file .releases.env -f compose.production.yaml)
"${compose[@]}" config --quiet
work=$(mktemp -d "$root/.deploy-work.XXXXXX")
trap 'rm -rf -- "$work"' EXIT

update_release() {
  python3 - .releases.env "$key" "$1" <<'PY'
import os, pathlib, sys, tempfile
path, key, value = pathlib.Path(sys.argv[1]), sys.argv[2], sys.argv[3]
lines = path.read_text().splitlines()
prefix = key + "="
lines = [line for line in lines if not line.startswith(prefix)]
lines.append(prefix + value)
fd, tmp = tempfile.mkstemp(dir=path.parent, prefix=".releases.")
with os.fdopen(fd, "w") as output:
    output.write("\n".join(lines) + "\n")
os.replace(tmp, path)
PY
}

diagnostics() {
  "${compose[@]}" ps "$service" || true
  "${compose[@]}" logs --tail=120 "$service" || true
}

verify_response() {
  local url=$1 expected_revision=$2
  curl --fail --silent --show-error --connect-timeout 2 --max-time 5 "$url" -o "$work/response.json" || return 1
  python3 - "$work/response.json" "$marker" "$expected_revision" <<'PY'
import json, sys
try:
    data = json.load(open(sys.argv[1]))
    assert data["service"] == sys.argv[2]
    if sys.argv[3]:
        assert data["revision"] == sys.argv[3]
    else:
        assert data["status"] == "UP"
except (ValueError, KeyError, AssertionError, OSError):
    sys.exit(1)
PY
}

wait_ready() {
  local revision_endpoint=release.json
  [[ "$service" != backend ]] || revision_endpoint=health
  for attempt in {1..40}; do
    if verify_response "http://127.0.0.1:$port/health" "" &&
       verify_response "http://127.0.0.1:$port/$revision_endpoint" "$revision"; then
      return 0
    fi
    echo "Waiting for $service ($attempt/40)"
    sleep 3
  done
  return 1
}

previous_id=$("${compose[@]}" ps -a -q "$service")
previous_image=
if [[ -n "$previous_id" ]]; then
  previous_image=$(docker inspect --format '{{.Config.Image}}' "$previous_id")
elif [[ "$bootstrap" != --bootstrap ]]; then
  fail "No baseline container for $service. First installation requires --bootstrap."
fi

# Pull and validate the candidate before changing the stored release or container.
docker pull "$image"
actual_revision=$(docker inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' "$image")
[[ "$actual_revision" == "$revision" ]] || fail "Candidate image revision does not match the tested commit"

if [[ "$service" == backend ]]; then
  mkdir -p backups
  backup="backups/database-$(date -u +%Y%m%dT%H%M%SZ)-$revision.dump"
  "${compose[@]}" exec -T db sh -eu -c 'pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB"' > "$work/database.dump"
  [[ -s "$work/database.dump" ]] || fail "Empty database backup"
  "${compose[@]}" exec -T db pg_restore --list < "$work/database.dump" > /dev/null
  mv -- "$work/database.dump" "$backup"
  echo "Pre-migration database backup: $backup"
fi

update_release "$image"
successful=false
running_image_matches() {
  local running_id expected_id running_image
  running_id=$("${compose[@]}" ps -q "$service") || return 1
  [[ -n "$running_id" ]] || return 1
  expected_id=$(docker inspect --format '{{.Id}}' "$image") || return 1
  running_image=$(docker inspect --format '{{.Image}}' "$running_id") || return 1
  [[ "$running_image" == "$expected_id" ]]
}
if "${compose[@]}" up -d --no-deps --no-build --force-recreate --pull never "$service" &&
   wait_ready && running_image_matches; then
    if [[ "$bootstrap" == --bootstrap ]]; then
      successful=true
      echo "Bootstrap ready locally; validate DNS/TLS and public routing before enabling CI deploys."
    else
      revision_endpoint=release.json
      [[ "$service" != backend ]] || revision_endpoint=health
      # The proxy can take a few seconds to settle after container recreation.
      for attempt in {1..6}; do
        if verify_response "$public/health" "" &&
           verify_response "$public/$revision_endpoint" "$revision"; then
          successful=true
          break
        fi
        sleep 3
      done
    fi
fi

if [[ "$successful" != true ]]; then
  diagnostics
  if [[ "$service" == backend ]]; then
    echo "Backend failed. No automatic rollback: Flyway may have changed the database." >&2
    echo "Preserve the backup and inspect migration/application logs before recovery." >&2
  elif [[ -n "$previous_image" ]]; then
    update_release "$previous_image"
    if "${compose[@]}" up -d --no-deps --no-build --force-recreate --pull never "$service"; then
      echo "Recreated the previous frontend image; verify its availability. Deployment is still failed." >&2
    else
      echo "Frontend rollback also failed; manual recovery is required." >&2
    fi
  fi
  fail "$service did not pass revision, readiness and routing checks"
fi

printf '%s %s %s %s\n' "$(date -u +%FT%TZ)" "$service" "$revision" "$image" >> deployments.log
echo "Deployed $service at $revision ($image)"
