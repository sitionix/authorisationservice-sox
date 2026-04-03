# Dev VM Deploy

## Model
- Push to `develop` triggers `Dev Deploy On Push`.
- A pull request comment can trigger `Deploy On Comment` against the PR head branch:
  ```text
  /deploy service --name authorisationservice-sox --env dev
  ```
- GitHub Actions builds and publishes the runtime image, uploads a release bundle to the VM, and performs the rollout over SSH.
- The VM is runtime-only. The workflow does not use `git pull` and does not require manual VM edits.
- The shared PR comment workflow routes only one target per command:
  - `/deploy service ...` runs service deploy
  - `/deploy db ...` runs DB migration

## GitHub Environment
Use GitHub Environment `dev`.

### Secrets
- `DEPLOY_VM_HOST`
- `DEPLOY_VM_USER`
- `DEPLOY_VM_SSH_PRIVATE_KEY`
- `GHCR_PULL_USERNAME`
- `GHCR_PULL_TOKEN`
- `AUTHS_SOX_DB_PASSWORD`
- `AUTH_JWT_PRIVATE_KEY`
- `AUTH_JWT_PUBLIC_KEY`
- `SECURITY_EMAIL_VERIFICATION_HMAC_SECRET`

### Vars
- `DEPLOY_VM_PORT`

### Repository Vars
- `MAVEN_REPOSITORY_USERNAME`

### Repository Secrets
- `MAVEN_REPOSITORY_TOKEN`

## VM Runtime Contract
- Docker network: `sitionix-dev`
- Container name: `authorisationservice-sox`
- Docker network alias: `authorisationservice-sox`
- Host-only bind: `127.0.0.1:9090 -> 9090`
- Internal base URL for downstream services: `http://authorisationservice-sox:9090/authsox`
- VM-local verification URL: `http://127.0.0.1:9090/authsox/.well-known/jwks.json`

## Files Materialized on the VM
- Runtime root: `/opt/sitionix/runtime/authorisationservice-sox`
- Service env file:
  - `/opt/sitionix/runtime/authorisationservice-sox/shared/authorisationservice-sox.dev.env`
- JWT key files:
  - `/opt/sitionix/runtime/authorisationservice-sox/shared/keys/jwt-private.pem`
  - `/opt/sitionix/runtime/authorisationservice-sox/shared/keys/jwt-public.pem`
- Shared internal auth env file, owned by infra:
  - `/opt/sitionix/runtime/shared/dev-internal-auth.env`
- Release backups:
  - `/opt/sitionix/backups/authorisationservice-sox/releases/<release-id>/release-manifest.json`

## Runtime Environment Values
The deploy workflow materializes these runtime values for the container:
- `SPRING_PROFILES_ACTIVE=dev`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/auths_sox`
- `SPRING_DATASOURCE_USERNAME=authssox_app`
- `SPRING_DATASOURCE_PASSWORD` from `AUTHS_SOX_DB_PASSWORD`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092`
- `AUTH_JWT_PRIVATE_KEY_PATH`
- `AUTH_JWT_PUBLIC_KEY_PATH`
- `SECURITY_EMAIL_VERIFICATION_HMAC_SECRET`
- `FORGE_SECURITY_DEV_JWT_SECRET` from the shared infra-owned file
- JWT key id falls back to the application default `local-dev`

## Verification
- Remote rollout waits for the private JWKS endpoint:
  - `GET /authsox/.well-known/jwks.json`
- Workflow smoke verification opens an SSH tunnel to `127.0.0.1:9090` on the VM and checks:
  - canonical JWKS endpoint
  - alias JWKS endpoint
  - both responses are identical
- the returned JWKS contains at least one key

## Shared Maven Contract
- The canonical Maven settings template is owned by `sitionix-infra`:
  - `contracts/shared/maven/settings.xml.template`
- This workflow checks out that shared template at runtime and injects:
  - repository variable `MAVEN_REPOSITORY_USERNAME`
  - repository secret `MAVEN_REPOSITORY_TOKEN`
- The full `settings.xml` blob is no longer stored as a secret in this repository.

## What This Workflow Does Not Do
- It does not run Flyway DB migration.
- It does not deploy BFF or any other service.
- It does not expose auth publicly on `0.0.0.0`.
