# Production-only deployment

## Branches and environments

- `dev`: local development and local testing. No VPS environment.
- `main`: tested production releases, published to GHCR and deployed to the one VPS stack.
- `staging`: retire only after `main` is published and selected as GitHub's default branch.
- CI checks pushes/PRs for `dev` and `main` if either is shared on GitHub. Only `main` can publish/deploy. You do not need to push `dev`.
- Items 5-9 of the Citizen launch follow-up are separate work; no staging stack is required for them.

| Repository | Compose service | Public host | VPS loopback |
| --- | --- | --- | --- |
| sheria-connect-api | backend | api.sheriaconnect.co.tz | 6001 |
| sheria-connect (existing portal repo) | frontend | admin.sheriaconnect.co.tz | 4101 |
| sheria-connect-app | citizen | app.sheriaconnect.co.tz | 4001 |
| sheria-connect-website | website | sheriaconnect.co.tz / www | 4201 |

The old portal keeps its image package/container/service names, but not its old port. Development WSL Nginx remains separate and unchanged.

## What the workflow does

1. API: JDK 21, a fresh PostgreSQL 16 database, all Maven tests/package checks, deployment-helper tests. No developer database or staff bootstrap accounts.
2. Frontends: Node 22, lockfile installation, lint, tests, explicit production build.
3. On `main`, publish a full-commit-SHA image, reuse it on retries, and pass its immutable digest to deployment.
4. Deploy only when the repository variable `PRODUCTION_DEPLOY_ENABLED=true`. Leave it unset/false during preparation.
5. Use pinned SSH host keys and the server-installed helper. A shared VPS lock serializes all repositories.
6. Pull and verify the image before changing release state. Back up the database before every backend recreation.
7. Recreate only the selected service. Confirm localhost readiness, actual running image, public host, service marker and revision.
8. A failed frontend rolls back to its prior image, with the workflow still failed. Backend failures do NOT automatically roll back potentially incompatible Flyway changes.

All three frontend Docker contexts exclude local env files but include the reviewed, PUBLIC `.env.production` settings. Never put secrets in a `VITE_` variable.

## First: preserve server state

Do not run `docker compose down -v`, delete volumes, reset the database or move evidence.
Keep the stack at `/srv/apps/sheria-connect` during this cutover.

Run these read-only commands on the VPS and record the results:

```bash
cd /srv/apps/sheria-connect
docker inspect sheria-connect-db --format '{{ index .Config.Labels "com.docker.compose.project" }}'
docker inspect sheria-connect-db --format '{{ range .Mounts }}{{ if eq .Destination "/var/lib/postgresql/data" }}{{ .Name }}{{ end }}{{ end }}'
docker inspect sheria-connect-cloudbeaver --format '{{ range .Mounts }}{{ if eq .Destination "/opt/cloudbeaver/workspace" }}{{ .Name }}{{ end }}{{ end }}'
docker inspect sheria-connect-cloudbeaver --format '{{.Config.Image}}'
docker inspect sheria-connect-backend --format '{{.Config.Image}}'
docker inspect sheria-connect-frontend --format '{{.Config.Image}}'
docker inspect sheria-connect-backend --format '{{json .Mounts}}'
docker compose version
sudo nginx -t
```

Use the actual project/volume/image values, not guessed names. `compose.production.yaml` uses external volumes so a typo fails rather than creating an empty database. Evidence uses the existing absolute host directory and fails if it is missing.

Before changing configuration, take protected backups:

```bash
umask 077
stamp=$(date -u +%Y%m%dT%H%M%SZ)
mkdir -p backups
cp .env "backups/env-$stamp"
cp docker-compose.yaml "backups/compose-$stamp.yaml"
docker compose exec -T db sh -eu -c 'pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB"' > "backups/database-$stamp.dump"
test -s "backups/database-$stamp.dump"
docker compose exec -T db pg_restore --list < "backups/database-$stamp.dump" >/dev/null
tar -czf "backups/evidence-$stamp.tar.gz" -C uploads evidence
sudo cp /etc/nginx/sites-available/sheriaconnect "/etc/nginx/sites-available/sheriaconnect.backup-$stamp"
```

Copy backups to a secure off-server location as well. Database backups contain sensitive reports; never expose `backups` or `uploads` through Nginx.

## Prepare GitHub before the first main push

For EACH of the four repositories:

- Create the `production` GitHub Environment, restrict it to `main`, and configure approval if desired.
- Repository variable: `PRODUCTION_DEPLOY_ENABLED=false`.
- Production environment variable: `SERVER_PATH=/srv/apps/sheria-connect`.
- Production secrets: `SERVER_HOST`, `SERVER_USER`, `SSH_PRIVATE_KEY`, `SSH_KNOWN_HOSTS`.
- Verify the SSH host fingerprint through a trusted VPS console/previously verified SSH connection; do not blindly trust `ssh-keyscan` output.
- Use a dedicated deployment key/user with access to this directory and Docker.
- Ensure each repository's Actions `GITHUB_TOKEN` can write its own GHCR package. For the existing backend/frontend packages, grant the repository Actions access in package settings if necessary. `NEW_PAT` is no longer used by CI.
- On the VPS, log in to GHCR once as the deployment user with a read-packages credential. Do not use a publishing PAT on the VPS.

Review and commit each repository's work before publishing it. Do not accidentally omit lockfiles, public env files or assets from the new web repositories.
Freeze pushes to the old `master`/`staging` branches during cutover: their historical workflow still deploys from `staging` until it is retired on GitHub.

For an existing repo, after review/commit on `dev`:

```powershell
git switch main
git merge --ff-only dev
git push -u origin main
git switch dev
```

For a new repo whose first commit is on `dev`:

```powershell
git branch main dev
git push -u origin main
```

If fast-forward fails, stop and inspect divergence; never force-push to make a release.
Set GitHub's default branch to `main`, configure branch protection/CI requirements, update collaborators, then archive and delete old remote branches. Fetch before deleting; preserve any newly added remote commits first.

```powershell
git fetch origin --prune
git tag backup/old-remote-master origin/master
git tag backup/old-remote-staging origin/staging
# Push these archive tags if long-term shared recovery is wanted.
git push origin backup/old-remote-master backup/old-remote-staging
# Only after checking their commits are preserved and main is the default:
git push origin --delete staging
git push origin --delete master
```

Skip commands for branches that do not exist. Do not delete a branch still selected as the default. Two teammates editing shared `main` must pull/fetch before release.

With deployments disabled, a passing `main` workflow publishes the candidate digest without touching the VPS. Collect all four digests and their full commit SHAs from the workflow summaries.

## Install the production files

Copy the versioned files, not entire repositories, to the VPS:

- `deploy/compose.production.yaml` -> `/srv/apps/sheria-connect/compose.production.yaml`
- `deploy/ops/deploy.sh` -> `/srv/apps/sheria-connect/ops/deploy.sh`
- `deploy/releases.env.example` -> `.releases.env`, adjusting the baseline backend/admin images to the actual running ones.
- Keep `env.production.example` as reference only. Edit the EXISTING `.env` to add its required deployment settings.

Preserve DB credentials, JWT signing secret, SMTP credentials and evidence location.
Do not rotate the MFA or tracking-token keys during this migration. If MFA previously fell back to `JWT_SECRET`, set `MFA_ENCRYPTION_KEY` to that exact previous value, not a new random key. Keep the existing `TRACKING_TOKEN_DERIVATION_KEY` unchanged.

Production auth emails temporarily go to the admin host: that project already implements public verification, password-reset and invitation pages. The website is not an authentication app, and the new Citizen web app does not yet implement all these routes. The legacy root-domain `/auth/` redirects preserve old links.

Blank bootstrap emails/passwords once existing admins are confirmed: the current bootstrap loader otherwise reapplies configured passwords at every startup. Do not set `BUILD_SHA` in `.env`; the image supplies its revision.

```bash
cd /srv/apps/sheria-connect
chmod 600 .env .releases.env
chmod 750 ops/deploy.sh
bash -n ops/deploy.sh
docker compose --env-file .env --env-file .releases.env -f compose.production.yaml config --quiet
```

The deploy user must retain write access to the directory/state files and evidence. Validate existing bind-mount permissions; do not use `chmod 777`.

## DNS, TLS and cutover

1. Point `app.sheriaconnect.co.tz` and `admin.sheriaconnect.co.tz` to the VPS. Check for conflicting AAAA records too.
2. Install `nginx/new-hostnames-http.conf` as a temporary enabled site alongside the existing site. Create `/var/www/letsencrypt`.
3. Test/reload Nginx, then issue the new certificate WITHOUT stopping Nginx or other VPS projects:

```bash
sudo mkdir -p /var/www/letsencrypt
sudo nginx -t && sudo systemctl reload nginx
sudo certbot certonly --webroot -w /var/www/letsencrypt \
  --cert-name sheria-connect-apps \
  -d app.sheriaconnect.co.tz -d admin.sheriaconnect.co.tz
```

The existing certificate at `/etc/letsencrypt/live/sheriaconnect.co.tz` must still cover root, www and API. Inspect `sudo certbot certificates`; adjust paths if your certificate names differ.

4. In a short announced maintenance window, bootstrap each tested digest using the helper. Replace DIGEST and FULL_SHA below with actual workflow outputs:

```bash
cd /srv/apps/sheria-connect
bash ops/deploy.sh backend ghcr.io/alim-core/sheria-connect-backend@sha256:DIGEST FULL_SHA --bootstrap
bash ops/deploy.sh frontend ghcr.io/alim-core/sheria-connect-frontend@sha256:DIGEST FULL_SHA --bootstrap
bash ops/deploy.sh citizen ghcr.io/alim-core/sheria-connect-app@sha256:DIGEST FULL_SHA --bootstrap
bash ops/deploy.sh website ghcr.io/alim-core/sheria-connect-website@sha256:DIGEST FULL_SHA --bootstrap
```

Bootstrapping the portal frees port 4001 by moving it to 4101. During this short window the old website proxy must not remain the final configuration: it still points at 4001.

5. Replace the old `sheriaconnect` site with `nginx/sheriaconnect.conf`, remove only the temporary new-hostnames symlink, then `sudo nginx -t && sudo systemctl reload nginx`.
6. Check all public `/health` endpoints, all frontend `/release.json` endpoints and API `/health` revision against the four candidate SHAs. Check `www` too, TLS redirects and an OPTIONS request from both app/admin origins.
7. Smoke-test website navigation, staff password+TOTP login and refresh, a verification link, Citizen registration, evidence upload/download and provider access using current APKs. Confirm existing reports and files still exist.
8. Run `sudo certbot renew --dry-run`. Enable `PRODUCTION_DEPLOY_ENABLED=true` in each repo only after these checks. A manual workflow run on `main` with `deploy=true` repeats normal public-routing checks.

Do not use the old `docker-compose.yaml` for routine updates after cutover. Keep it only as a recovery reference. Use the new file with both env files. Existing CloudBeaver is left running; intentional updates use the `tools` profile and its inspected existing volume.

## Routine release and recovery

Web refresh sessions now use separate host-only HttpOnly cookies: `refresh_token_staff` for the portal and `refresh_token_citizen` for the Citizen web app. Login, refresh and logout identify their context using `X-Client-Type: WEB` and `X-Active-Context: STAFF` or `CITIZEN`. Refresh validates both against the stored session before rotation, including during the reuse grace window. Staff still requires TOTP; Citizen web login does not bypass Staff authentication. Provider participation remains mobile-only.

Deploy the backend before the updated portal and Citizen web bundles. Existing `refresh_token` cookies migrate only on a successful refresh for their original context, then expire; an unrelated context cannot consume or clear them. Users whose old shared cookie was already overwritten must sign in again. No database migration, key rotation or APK update is required for this cookie repair. Mobile login/refresh continues using body tokens, not web cookies.

Verify the repair in one browser profile: sign into Citizen web and Staff portal, let access tokens expire, and navigate in both. Check that each refresh returns its own active context. Sign out of Citizen web and confirm Staff stays signed in; repeat in the opposite direction. In browser storage, both cookies should be HttpOnly, Secure in production, and scoped to the API host without a parent-domain attribute.

The Citizen web repository currently contains an authentication shell, not the complete reporting experience. Deploying its container does not mean the Citizen web MVP is feature-complete.

Work on local `dev`, run checks, commit, then fast-forward `main` to the tested revision and push `main`. CI never deploys development. Avoid unrelated cross-repository releases in one go; backend additive changes first, clients second.

No VPS source pulls/builds, no global `docker compose pull/up`, and no `docker image prune` in deployment. Keep at least the previous images and protected DB/evidence backups.

On a failed backend release, inspect `docker compose ... logs backend`, Flyway results and the saved database dump before choosing a compatible code rollback or restoring the DB during maintenance. A code rollback cannot undo a schema migration. Restoring DB alone may also make evidence metadata/files inconsistent, so coordinate their snapshots.

Updates to this shared helper/Compose/Nginx bundle are deliberate operational changes: review and install them on the server separately. Frontend pipelines never overwrite the shared server configuration.

## Local verification

Run `mvn verify` on JDK 21 against a dedicated PostgreSQL database with `SPRING_PROFILES_ACTIVE=ci` and the `TEST_DATABASE_*` variables, never against production. Set a CI-only `JWT_SECRET` in the process environment.

Run `npm run lint`, `npm test`, `npm run build:vps` in each web repository.
Validate workflow YAML with Actionlint and run `python3 -m unittest discover -s deploy/tests -v` on Linux/WSL.
For the optional local Docker smoke check, build the four Dockerfiles with `--build-arg BUILD_SHA=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa`, tagging `sheria-connect-api:deployment-check`, `sheria-connect-admin:deployment-check`, `sheria-connect-app:deployment-check` and `sheria-connect-website:deployment-check`. Run `bash deploy/tests/verify-local.sh` in Linux/WSL. It uses disposable containers/certificates, validates Compose/Nginx, starts production API against a fresh PostgreSQL database, checks host CORS, and cleans up its containers/network. It never touches the VPS or existing database.
