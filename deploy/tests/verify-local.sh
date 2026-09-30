#!/usr/bin/env bash
# Local workspace smoke checks after building the four :deployment-check images.
set -Eeuo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
work=$(mktemp -d)
containers=()
network=
cleanup() {
  for container in "${containers[@]}"; do docker rm -f "$container" >/dev/null 2>&1 || true; done
  [[ -z "$network" ]] || docker network rm "$network" >/dev/null 2>&1 || true
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

# Boot the real production image against a fresh disposable database, no .env.
network="sheria-smoke-network-$$"
docker network create "$network" >/dev/null
db="sheria-smoke-db-$$"
docker run -d --name "$db" --network "$network" \
  --tmpfs /var/lib/postgresql/data:rw,size=256m \
  -e POSTGRES_DB=sheria_connect_smoke -e POSTGRES_USER=sheria_ci \
  -e POSTGRES_PASSWORD=ci-only-not-production postgres:16-alpine >/dev/null
containers+=("$db")
for attempt in {1..30}; do
  if docker exec "$db" pg_isready -U sheria_ci -d sheria_connect_smoke >/dev/null 2>&1; then break; fi
  sleep 1
done
api="sheria-smoke-api-$$"
docker run -d --name "$api" --network "$network" -p 127.0.0.1::6001 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST="$db" -e DB_NAME=sheria_connect_smoke -e DB_USERNAME=sheria_ci \
  -e DB_PASSWORD=ci-only-not-production \
  -e JWT_SECRET=ci-only-signing-secret-at-least-sixty-four-characters-long-not-production \
  -e MFA_ENCRYPTION_KEY=ci-only-mfa-not-production \
  -e TRACKING_TOKEN_DERIVATION_KEY=ci-only-tracking-not-production \
  sheria-connect-api:deployment-check >/dev/null
containers+=("$api")
port=$(docker port "$api" 6001/tcp | cut -d: -f2)
ready=false
for attempt in {1..60}; do
  if curl -fsS --connect-timeout 2 --max-time 5 "http://127.0.0.1:$port/health" > "$work/api.json" 2>/dev/null; then ready=true; break; fi
  sleep 2
done
if [[ "$ready" != true ]]; then docker logs --tail=80 "$api"; exit 1; fi
python3 - "$work/api.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
assert data["status"] == "UP"
assert data["service"] == "sheria-connect-api"
assert data["revision"] == "a" * 40
PY
for host in app admin; do
  origin="https://$host.sheriaconnect.co.tz"
  curl -fsS -D "$work/cors.headers" -o /dev/null --max-time 5 \
    -X OPTIONS "http://127.0.0.1:$port/auth/mfa/verify" \
    -H "Origin: $origin" -H 'Access-Control-Request-Method: POST' \
    -H 'Access-Control-Request-Headers: content-type,x-client-type,x-active-context'
  grep -Fq "Access-Control-Allow-Origin: $origin" "$work/cors.headers"
done
echo "Fresh production API startup, migrations, readiness and app/admin CORS checks passed."
