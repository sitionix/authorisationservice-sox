#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
payload_dir="$(cd "${script_dir}/.." && pwd)"

# shellcheck source=/dev/null
source "${payload_dir}/release.env"

require_env() {
  local variable_name="$1"
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Missing required environment variable: ${variable_name}" >&2
    exit 1
  fi
}

assert_absolute_path() {
  local path_value="$1"
  local allowed_prefix="$2"

  if [[ "${path_value}" != "${allowed_prefix}"* ]]; then
    echo "Path ${path_value} must stay under ${allowed_prefix}" >&2
    exit 1
  fi
}

wait_for_url() {
  local target_url="$1"
  local max_attempts="${2:-30}"
  local attempt=1

  while (( attempt <= max_attempts )); do
    if curl -fsS "${target_url}" >/dev/null; then
      return 0
    fi
    sleep 2
    ((attempt++))
  done

  return 1
}

rollback_previous_container() {
  if [[ -z "${previous_container_name:-}" ]]; then
    return
  fi
  docker rm -f "${SITIONIX_CONTAINER_NAME}" >/dev/null 2>&1 || true
  docker rename "${previous_container_name}" "${SITIONIX_CONTAINER_NAME}"
  docker start "${SITIONIX_CONTAINER_NAME}" >/dev/null
}

for variable_name in \
  SITIONIX_RELEASE_ID \
  SITIONIX_IMAGE_REF \
  SITIONIX_CONTAINER_NAME \
  SITIONIX_NETWORK_ALIAS \
  SITIONIX_BIND_ADDRESS \
  SITIONIX_HOST_PORT \
  SITIONIX_CONTAINER_PORT \
  SITIONIX_ACTIVE_PROFILE \
  SITIONIX_DOCKER_NETWORK \
  SITIONIX_RUNTIME_ROOT \
  SITIONIX_BACKUP_ROOT \
  SITIONIX_SHARED_SECRET_ENV_PATH \
  SITIONIX_SERVICE_ENV_PATH \
  SITIONIX_KEYS_DIR \
  SITIONIX_JWT_PRIVATE_KEY_PATH \
  SITIONIX_JWT_PUBLIC_KEY_PATH \
  SITIONIX_LOCAL_READINESS_URL \
  SITIONIX_LOCAL_HEALTH_URL \
  SITIONIX_LOCAL_JWKS_URL \
  SITIONIX_DB_URL \
  SITIONIX_DB_USERNAME \
  SITIONIX_KAFKA_BOOTSTRAP_SERVERS \
  GHCR_PULL_USERNAME \
  GHCR_PULL_TOKEN \
  AUTHS_SOX_DB_PASSWORD \
  AUTH_JWT_PRIVATE_KEY \
  AUTH_JWT_PUBLIC_KEY \
  SECURITY_EMAIL_VERIFICATION_HMAC_SECRET; do
  require_env "${variable_name}"
done

assert_absolute_path "${SITIONIX_RUNTIME_ROOT}" "/opt/sitionix/"
assert_absolute_path "${SITIONIX_BACKUP_ROOT}" "/opt/sitionix/"
assert_absolute_path "${SITIONIX_SHARED_SECRET_ENV_PATH}" "/opt/sitionix/"
assert_absolute_path "${SITIONIX_SERVICE_ENV_PATH}" "/opt/sitionix/"
assert_absolute_path "${SITIONIX_KEYS_DIR}" "/opt/sitionix/"
assert_absolute_path "${SITIONIX_JWT_PRIVATE_KEY_PATH}" "/opt/sitionix/"
assert_absolute_path "${SITIONIX_JWT_PUBLIC_KEY_PATH}" "/opt/sitionix/"

if [[ ! -f "${SITIONIX_SHARED_SECRET_ENV_PATH}" ]]; then
  echo "Missing shared internal auth secret file: ${SITIONIX_SHARED_SECRET_ENV_PATH}" >&2
  exit 1
fi

release_backup_root="${SITIONIX_BACKUP_ROOT}/releases/${SITIONIX_RELEASE_ID}"
shared_dir="$(dirname "${SITIONIX_SERVICE_ENV_PATH}")"

mkdir -p "${release_backup_root}" "${shared_dir}" "${SITIONIX_KEYS_DIR}"
cp "${payload_dir}/release-manifest.json" "${release_backup_root}/release-manifest.json"

printf '%s\n' "${GHCR_PULL_TOKEN}" | docker login ghcr.io -u "${GHCR_PULL_USERNAME}" --password-stdin
docker pull "${SITIONIX_IMAGE_REF}"

printf '%s\n' "${AUTH_JWT_PRIVATE_KEY}" > "${SITIONIX_JWT_PRIVATE_KEY_PATH}"
printf '%s\n' "${AUTH_JWT_PUBLIC_KEY}" > "${SITIONIX_JWT_PUBLIC_KEY_PATH}"
chmod 0600 "${SITIONIX_JWT_PRIVATE_KEY_PATH}" "${SITIONIX_JWT_PUBLIC_KEY_PATH}"

{
  printf 'SPRING_PROFILES_ACTIVE=%s\n' "${SITIONIX_ACTIVE_PROFILE}"
  printf 'SPRING_DATASOURCE_URL=%s\n' "${SITIONIX_DB_URL}"
  printf 'SPRING_DATASOURCE_USERNAME=%s\n' "${SITIONIX_DB_USERNAME}"
  printf 'SPRING_DATASOURCE_PASSWORD=%s\n' "${AUTHS_SOX_DB_PASSWORD}"
  printf 'SPRING_KAFKA_BOOTSTRAP_SERVERS=%s\n' "${SITIONIX_KAFKA_BOOTSTRAP_SERVERS}"
  printf 'AUTH_JWT_PRIVATE_KEY_PATH=%s\n' "${SITIONIX_JWT_PRIVATE_KEY_PATH}"
  printf 'AUTH_JWT_PUBLIC_KEY_PATH=%s\n' "${SITIONIX_JWT_PUBLIC_KEY_PATH}"
  printf 'SECURITY_EMAIL_VERIFICATION_HMAC_SECRET=%s\n' "${SECURITY_EMAIL_VERIFICATION_HMAC_SECRET}"
} > "${SITIONIX_SERVICE_ENV_PATH}"
chmod 0600 "${SITIONIX_SERVICE_ENV_PATH}"

if ! docker run --rm \
  --entrypoint sh \
  --env-file "${SITIONIX_SHARED_SECRET_ENV_PATH}" \
  --env-file "${SITIONIX_SERVICE_ENV_PATH}" \
  --mount "type=bind,src=${SITIONIX_KEYS_DIR},dst=${SITIONIX_KEYS_DIR},readonly" \
  "${SITIONIX_IMAGE_REF}" \
  -lc '
    test -r "${AUTH_JWT_PRIVATE_KEY_PATH:?}"
    test -r "${AUTH_JWT_PUBLIC_KEY_PATH:?}"
  '; then
  echo "JWT key files are not readable from the container runtime." >&2
  exit 1
fi

if ! docker network inspect "${SITIONIX_DOCKER_NETWORK}" >/dev/null 2>&1; then
  docker network create "${SITIONIX_DOCKER_NETWORK}" >/dev/null
fi

previous_container_name=""
staged_previous_name="${SITIONIX_CONTAINER_NAME}.previous"
docker rm -f "${staged_previous_name}" >/dev/null 2>&1 || true

if docker container inspect "${SITIONIX_CONTAINER_NAME}" >/dev/null 2>&1; then
  previous_container_name="${staged_previous_name}"
  docker stop "${SITIONIX_CONTAINER_NAME}" >/dev/null
  docker rename "${SITIONIX_CONTAINER_NAME}" "${staged_previous_name}"
fi

docker rm -f "${SITIONIX_CONTAINER_NAME}" >/dev/null 2>&1 || true

if ! docker run -d \
  --name "${SITIONIX_CONTAINER_NAME}" \
  --restart unless-stopped \
  --network "${SITIONIX_DOCKER_NETWORK}" \
  --network-alias "${SITIONIX_NETWORK_ALIAS}" \
  -p "${SITIONIX_BIND_ADDRESS}:${SITIONIX_HOST_PORT}:${SITIONIX_CONTAINER_PORT}" \
  --env-file "${SITIONIX_SHARED_SECRET_ENV_PATH}" \
  --env-file "${SITIONIX_SERVICE_ENV_PATH}" \
  --mount "type=bind,src=${SITIONIX_KEYS_DIR},dst=${SITIONIX_KEYS_DIR},readonly" \
  "${SITIONIX_IMAGE_REF}" >/dev/null; then
  rollback_previous_container
  exit 1
fi

if ! wait_for_url "${SITIONIX_LOCAL_READINESS_URL}" 30; then
  docker logs "${SITIONIX_CONTAINER_NAME}" --tail 200 >&2 || true
  rollback_previous_container
  exit 1
fi

if ! wait_for_url "${SITIONIX_LOCAL_HEALTH_URL}" 10; then
  docker logs "${SITIONIX_CONTAINER_NAME}" --tail 200 >&2 || true
  rollback_previous_container
  exit 1
fi

if [[ -n "${previous_container_name}" ]]; then
  docker rm -f "${previous_container_name}" >/dev/null 2>&1 || true
fi

docker ps --filter "name=^${SITIONIX_CONTAINER_NAME}$"
