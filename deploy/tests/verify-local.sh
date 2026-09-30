#!/usr/bin/env bash
# Local workspace smoke checks after building the four :deployment-check images.
set -Eeuo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
work=$(mktemp -d)
containers=()
cleanup() {
  for container in "${containers[@]}"; do docker rm -f "$container" >/dev/null 2>&1 || true; done
  rm -rf -- "$work"
}
trap cleanup EXIT
cp "$root/compose.production.yaml" "$work/compose.production.yaml"
cp "$root/env.production.example" "$work/.env"
cp "$root/releases.env.example" "$work/.releases.env"
docker compose --env-file "$work/.env" --env-file "$work/.releases.env" -f "$work/compose.production.yaml" config --quiet
bash -n "$root/ops/deploy.sh"

for service in admin app website; do
  image="sheria-connect-$service:deployment-check"
  docker run --rm --entrypoint nginx "$image" -t
  container="sheria-smoke-$service-$$"
  docker run -d --name "$container" -p 127.0.0.1::80 "$image" >/dev/null
  containers+=("$container")
  port=$(docker port "$container" 80/tcp | cut -d: -f2)
  for attempt in {1..20}; do
    if curl -fsS --connect-timeout 2 --max-time 5 "http://127.0.0.1:$port/health" > "$work/health.json"; then break; fi
    sleep 1
  done
  curl -fsS --max-time 5 "http://127.0.0.1:$port/release.json" > "$work/release.json"
  python3 - "$work" "$service" <<'PY'
import json, pathlib, sys
root, service = pathlib.Path(sys.argv[1]), sys.argv[2]
health = json.loads((root / "health.json").read_text())
release = json.loads((root / "release.json").read_text())
assert health == {"status": "UP", "service": "sheria-connect-" + service}
assert release == {"service": health["service"], "revision": "a" * 40}
PY
done

# Check host-Nginx syntax using disposable certificates, never the real private keys.
mkdir -p "$work/tls/live/sheriaconnect.co.tz" "$work/tls/live/sheria-connect-apps"
openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj /CN=deployment-smoke-test \
  -keyout "$work/tls/live/sheriaconnect.co.tz/privkey.pem" \
  -out "$work/tls/live/sheriaconnect.co.tz/fullchain.pem" >/dev/null 2>&1
cp "$work/tls/live/sheriaconnect.co.tz/"* "$work/tls/live/sheria-connect-apps/"
touch "$work/tls/options-ssl-nginx.conf"
openssl dhparam -dsaparam -out "$work/tls/ssl-dhparams.pem" 2048 >/dev/null 2>&1
docker run --rm --entrypoint nginx \
  -v "$root/nginx/sheriaconnect.conf:/etc/nginx/conf.d/default.conf:ro" \
  -v "$work/tls:/etc/letsencrypt:ro" \
  sheria-connect-website:deployment-check -t
echo "Compose, shell syntax, frontend runtime identity and host Nginx checks passed."
