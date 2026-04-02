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
- Add new migrations as flat, versioned SQL files like `V13__describe_change.sql`.
- Do not edit applied migration files; add a new version instead.
- This service owns its PostgreSQL schema; real datasource credentials must come from env/secrets (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
- Hibernate schema generation is not the source of truth; Flyway is.
- Existing Forge IT tests keep using their own schema/data setup under `boot/src/test/resources/forge-it/...`; the runtime migration workflow does not replace that test harness.

## DB migration workflow
- Migrations run only through a PR comment trigger, not on push.
- Command format:
  ```text
  /deploy db --name auths_sox --env dev
  ```
- The workflow accepts only the exact canonical DB name `auths_sox`.
- Supported environments are defined under `deploy/db-migrate/environments`.
- The workflow binds the migration job directly to the selected GitHub Environment.
- The workflow runs Flyway only; it does not deploy the service binary.

Required GitHub Environment variables:
- `AUTHS_SOX_DB_MIGRATION_URL`
- `AUTHS_SOX_DB_MIGRATION_USERNAME`

Required GitHub Environment secrets:
- `AUTHS_SOX_DB_MIGRATION_PASSWORD`
- `AUTH_TOKEN`

## Dev key generation (PEM)
```
mkdir -p ./keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out ./keys/jwt-private.pem
openssl rsa -in ./keys/jwt-private.pem -pubout -out ./keys/jwt-public.pem
```
