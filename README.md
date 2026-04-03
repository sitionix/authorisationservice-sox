# Authorisation Service (Sitionix)

## JWT signing (RS256)
- Access tokens are signed with RS256 and include a `kid` header.
- JWKS endpoint: `GET /.well-known/jwks.json` (alias: `GET /oauth2/v1/keys`).
- Only public key parameters are exposed; private keys are never returned.

## Configuration
Configure one signing key source (keystore or PEM) via env/config.

PEM example (default paths match `./keys`):
```
AUTH_JWT_KEY_ID=local-dev
AUTH_JWT_PRIVATE_KEY_PATH=./keys/jwt-private.pem
AUTH_JWT_PUBLIC_KEY_PATH=./keys/jwt-public.pem
```

PKCS12 example:
```
AUTH_JWT_KEY_ID=local-dev
AUTH_JWT_KEY_STORE_PATH=classpath:keystore.p12
AUTH_JWT_KEY_STORE_PASSWORD=changeit
AUTH_JWT_KEY_STORE_ALIAS=jwt
AUTH_JWT_KEY_STORE_KEY_PASSWORD=changeit
```

Verification keys for rotation can be added via `auth.tokens.jwt.verification-keys` entries
with a `key-id` and `public-key`/`public-key-path`.

## Database migrations
- Flyway migrations live in `db-migration` at the repository root.
- The migration control model also lives there in `db-migration/db-model.yaml`.
- Add new migrations as flat, versioned SQL files like `V13__describe_change.sql`.
- Do not edit applied migration files; add a new version instead.
- This service owns its PostgreSQL schema.
- Hibernate schema generation is not the source of truth; Flyway is.
- Existing Forge IT tests keep using their own schema/data setup under `boot/src/test/resources/forge-it/...`; the runtime migration workflow does not replace that test harness.

## DB migration workflow
- Migrations run only through a PR comment trigger, not on push.

- Command format:
  ```text
  /deploy db --name auths_sox --env dev
  ```
- The workflow accepts only the exact canonical DB name `auths_sox`.
- The command contract itself is enforced in the workflow.
- `db-migration/db-model.yaml` contains only the DB migration model:
  - canonical DB name
  - supported environments
  - direct Flyway URL and username
  - GitHub secret name for the Flyway password
- Migration connection settings are independent from `boot` runtime config; the workflow reads them from `db-model.yaml` and injects them into Flyway directly.
- The workflow binds the job to the same GitHub Environment as `--env`.
- The workflow runs Flyway only; it does not deploy the service binary.
- The workflow does not build the service; it runs standalone Flyway against the SQL files in `db-migration`.
- Pull request comment triggers run against the pull request head branch, not against `develop`.

Current `db-model.yaml` mapping for `dev` requires:
- Flyway URL `jdbc:postgresql://127.0.0.1:15432/auths_sox`
- Flyway username `authssox_app`
- GitHub Environment secret `AUTHS_SOX_DB_PASSWORD`
- SSH tunnel contract:
  - `DEPLOY_VM_HOST`
  - `DEPLOY_VM_USER`
  - `DEPLOY_VM_SSH_PRIVATE_KEY`
  - remote tunnel target `127.0.0.1:5432` on the VM host

## Dev service deploy
- Push to `develop` deploys the auth service to the `dev` VM through GitHub Actions.
- The deploy model is push-based: no `git pull` on the VM and no manual VM edits in the primary path.
- Private Maven artifact resolution uses the shared template owned by `sitionix-infra`, not a repo-local `settings.xml` blob secret.
- See `docs/dev-vm-deploy.md` for the full runtime, secret, and verification contract.


## Dev key generation (PEM)
```
mkdir -p ./keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out ./keys/jwt-private.pem
openssl rsa -in ./keys/jwt-private.pem -pubout -out ./keys/jwt-public.pem
```
