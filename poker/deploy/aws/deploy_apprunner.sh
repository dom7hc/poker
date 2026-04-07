#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

if [[ -f "${SCRIPT_DIR}/apprunner.env" ]]; then
  # shellcheck disable=SC1091
  source "${SCRIPT_DIR}/apprunner.env"
fi

AWS_REGION="${AWS_REGION:-us-east-1}"
ECR_REPOSITORY="${ECR_REPOSITORY:-poker-console}"
APP_RUNNER_SERVICE_NAME="${APP_RUNNER_SERVICE_NAME:-poker-console}"
APP_RUNNER_CPU="${APP_RUNNER_CPU:-1 vCPU}"
APP_RUNNER_MEMORY="${APP_RUNNER_MEMORY:-2 GB}"
PLAYER_COUNT="${PLAYER_COUNT:-4}"
MATCH_HANDS="${MATCH_HANDS:-5}"
WEB_CREDENTIALS="${WEB_CREDENTIALS:-}"
ALLOW_NO_AUTH="${ALLOW_NO_AUTH:-false}"
AUTO_DEPLOYMENTS_ENABLED="${AUTO_DEPLOYMENTS_ENABLED:-true}"
WAIT_FOR_RUNNING="${WAIT_FOR_RUNNING:-true}"
IMAGE_TAG="${IMAGE_TAG:-$(date +%Y%m%d%H%M%S)}"
APP_RUNNER_ECR_ACCESS_ROLE_NAME="${APP_RUNNER_ECR_ACCESS_ROLE_NAME:-AppRunnerECRAccessRole}"
APP_RUNNER_ECR_ACCESS_ROLE_ARN="${APP_RUNNER_ECR_ACCESS_ROLE_ARN:-}"

require_tool() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required tool: $1"
    exit 1
  fi
}

ensure_ecr_access_role() {
  if [[ -n "${APP_RUNNER_ECR_ACCESS_ROLE_ARN}" ]]; then
    return
  fi

  local role_arn
  role_arn="$(aws iam get-role --role-name "${APP_RUNNER_ECR_ACCESS_ROLE_NAME}" --query 'Role.Arn' --output text 2>/dev/null || true)"

  if [[ -z "${role_arn}" || "${role_arn}" == "None" ]]; then
    local trust_file
    trust_file="$(mktemp)"
    cat >"${trust_file}" <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "build.apprunner.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
    aws iam create-role \
      --role-name "${APP_RUNNER_ECR_ACCESS_ROLE_NAME}" \
      --assume-role-policy-document "file://${trust_file}" >/dev/null
    rm -f "${trust_file}"
  fi

  aws iam attach-role-policy \
    --role-name "${APP_RUNNER_ECR_ACCESS_ROLE_NAME}" \
    --policy-arn arn:aws:iam::aws:policy/service-role/AWSAppRunnerServicePolicyForECRAccess >/dev/null || true

  APP_RUNNER_ECR_ACCESS_ROLE_ARN="$(aws iam get-role --role-name "${APP_RUNNER_ECR_ACCESS_ROLE_NAME}" --query 'Role.Arn' --output text)"
}

wait_for_running() {
  local service_arn="$1"
  echo "Waiting for App Runner service to reach RUNNING..."
  for _ in $(seq 1 90); do
    local status
    status="$(aws apprunner describe-service --region "${AWS_REGION}" --service-arn "${service_arn}" --query 'Service.Status' --output text)"
    case "${status}" in
      RUNNING)
        return
        ;;
      CREATE_IN_PROGRESS|UPDATE_IN_PROGRESS|OPERATION_IN_PROGRESS)
        sleep 10
        ;;
      *)
        echo "Service status is ${status}. Check App Runner console."
        return
        ;;
    esac
  done
  echo "Timed out waiting for RUNNING status."
}

require_tool aws
require_tool docker

if [[ "${ALLOW_NO_AUTH}" != "true" && -z "${WEB_CREDENTIALS}" ]]; then
  echo "WEB_CREDENTIALS must be set (username:password) unless ALLOW_NO_AUTH=true."
  exit 1
fi

AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query 'Account' --output text --region "${AWS_REGION}")"
IMAGE_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}:${IMAGE_TAG}"

echo "Ensuring ECR repository exists: ${ECR_REPOSITORY}"
aws ecr describe-repositories --repository-names "${ECR_REPOSITORY}" --region "${AWS_REGION}" >/dev/null 2>&1 || \
  aws ecr create-repository --repository-name "${ECR_REPOSITORY}" --region "${AWS_REGION}" >/dev/null

echo "Logging into ECR..."
aws ecr get-login-password --region "${AWS_REGION}" | \
  docker login --username AWS --password-stdin "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

echo "Building image ${IMAGE_URI}"
docker build -t "${IMAGE_URI}" "${APP_ROOT}"

echo "Pushing image ${IMAGE_URI}"
docker push "${IMAGE_URI}"

ensure_ecr_access_role

source_cfg_file="$(mktemp)"
instance_cfg_file="$(mktemp)"

cat >"${source_cfg_file}" <<EOF
{
  "AuthenticationConfiguration": {
    "AccessRoleArn": "${APP_RUNNER_ECR_ACCESS_ROLE_ARN}"
  },
  "AutoDeploymentsEnabled": ${AUTO_DEPLOYMENTS_ENABLED},
  "ImageRepository": {
    "ImageIdentifier": "${IMAGE_URI}",
    "ImageRepositoryType": "ECR",
    "ImageConfiguration": {
      "Port": "8080",
      "RuntimeEnvironmentVariables": {
        "PLAYER_COUNT": "${PLAYER_COUNT}",
        "MATCH_HANDS": "${MATCH_HANDS}",
        "WEB_PORT": "8080",
        "WEB_CREDENTIALS": "${WEB_CREDENTIALS}"
      }
    }
  }
}
EOF

cat >"${instance_cfg_file}" <<EOF
{
  "Cpu": "${APP_RUNNER_CPU}",
  "Memory": "${APP_RUNNER_MEMORY}"
}
EOF

existing_arn="$(aws apprunner list-services --region "${AWS_REGION}" \
  --query "ServiceSummaryList[?ServiceName=='${APP_RUNNER_SERVICE_NAME}'].ServiceArn | [0]" \
  --output text)"

if [[ -z "${existing_arn}" || "${existing_arn}" == "None" ]]; then
  echo "Creating App Runner service: ${APP_RUNNER_SERVICE_NAME}"
  service_arn="$(aws apprunner create-service \
    --region "${AWS_REGION}" \
    --service-name "${APP_RUNNER_SERVICE_NAME}" \
    --source-configuration "file://${source_cfg_file}" \
    --instance-configuration "file://${instance_cfg_file}" \
    --query 'Service.ServiceArn' \
    --output text)"
else
  echo "Updating App Runner service: ${APP_RUNNER_SERVICE_NAME}"
  service_arn="${existing_arn}"
  aws apprunner update-service \
    --region "${AWS_REGION}" \
    --service-arn "${service_arn}" \
    --source-configuration "file://${source_cfg_file}" \
    --instance-configuration "file://${instance_cfg_file}" >/dev/null
fi

rm -f "${source_cfg_file}" "${instance_cfg_file}"

if [[ "${WAIT_FOR_RUNNING}" == "true" ]]; then
  wait_for_running "${service_arn}"
fi

service_url="$(aws apprunner describe-service --region "${AWS_REGION}" --service-arn "${service_arn}" --query 'Service.ServiceUrl' --output text)"

echo
echo "Deployment complete."
echo "Service ARN: ${service_arn}"
echo "Service URL: https://${service_url}"
echo "Image: ${IMAGE_URI}"
