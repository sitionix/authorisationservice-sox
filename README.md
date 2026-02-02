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

## Dev key generation (PEM)
```
mkdir -p ./keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out ./keys/jwt-private.pem
openssl rsa -in ./keys/jwt-private.pem -pubout -out ./keys/jwt-public.pem
```
