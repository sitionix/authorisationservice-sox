#!/usr/bin/env bash

set -euo pipefail

require_env() {
  local variable_name="$1"
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Missing required environment variable: ${variable_name}" >&2
    exit 1
  fi
}

for variable_name in \
  DEPLOY_VM_HOST \
  DEPLOY_VM_USER \
  DEPLOY_VM_SSH_PRIVATE_KEY \
  AUTH_JWT_KEY_ID \
  SITIONIX_RELEASE_ID; do
  require_env "${variable_name}"
done

key_path="$(mktemp)"
tunnel_pid=""
local_port="${SITIONIX_PRIVATE_TUNNEL_PORT:-19090}"
canonical_file="$(mktemp)"
alias_file="$(mktemp)"

cleanup() {
  if [[ -n "${tunnel_pid}" ]]; then
    kill "${tunnel_pid}" 2>/dev/null || true
    wait "${tunnel_pid}" 2>/dev/null || true
  fi
  rm -f "${key_path}" "${canonical_file}" "${alias_file}"
}

trap cleanup EXIT

printf '%s\n' "${DEPLOY_VM_SSH_PRIVATE_KEY}" > "${key_path}"
chmod 0600 "${key_path}"

ssh \
  -i "${key_path}" \
  -o IdentitiesOnly=yes \
  -o StrictHostKeyChecking=accept-new \
  -o ExitOnForwardFailure=yes \
  -o ConnectTimeout=15 \
  -o ServerAliveInterval=15 \
  -o ServerAliveCountMax=3 \
  -N \
  -L "${local_port}:127.0.0.1:9090" \
  -p "${DEPLOY_VM_PORT:-22}" \
  "${DEPLOY_VM_USER}@${DEPLOY_VM_HOST}" &
tunnel_pid=$!

for _ in $(seq 1 20); do
  if bash -c ">/dev/tcp/127.0.0.1/${local_port}" 2>/dev/null; then
    break
  fi
  sleep 1
done

if ! bash -c ">/dev/tcp/127.0.0.1/${local_port}" 2>/dev/null; then
  echo "Auth SSH tunnel did not become ready on 127.0.0.1:${local_port}" >&2
  exit 1
fi

canonical_url="http://127.0.0.1:${local_port}/authsox/.well-known/jwks.json"
alias_url="http://127.0.0.1:${local_port}/authsox/oauth2/v1/keys"

curl -fsS "${canonical_url}" > "${canonical_file}"
curl -fsS "${alias_url}" > "${alias_file}"

python3 - <<'PY' "${canonical_file}" "${alias_file}" "${AUTH_JWT_KEY_ID}" "${SITIONIX_RELEASE_ID}"
import json
import pathlib
import sys

canonical_path = pathlib.Path(sys.argv[1])
alias_path = pathlib.Path(sys.argv[2])
expected_kid = sys.argv[3]
release_id = sys.argv[4]

canonical = json.loads(canonical_path.read_text())
alias = json.loads(alias_path.read_text())

if canonical != alias:
    raise SystemExit("Canonical and alias JWKS responses differ")

keys = canonical.get("keys")
if not isinstance(keys, list) or not keys:
    raise SystemExit("JWKS response does not contain keys")

if not any(key.get("kid") == expected_kid for key in keys):
    raise SystemExit(f"JWKS response does not contain expected kid: {expected_kid}")

print(f"Auth private smoke passed for release {release_id}")
PY
