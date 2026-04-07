#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -f "${SCRIPT_DIR}/apprunner.env" ]]; then
  # shellcheck disable=SC1091
  source "${SCRIPT_DIR}/apprunner.env"
fi

AWS_REGION="${AWS_REGION:-us-east-1}"
APP_RUNNER_SERVICE_NAME="${APP_RUNNER_SERVICE_NAME:-poker-console}"
LOG_RETENTION_DAYS="${LOG_RETENTION_DAYS:-14}"

echo "Applying CloudWatch log retention for App Runner service: ${APP_RUNNER_SERVICE_NAME}"

groups=$(aws logs describe-log-groups \
  --region "${AWS_REGION}" \
  --log-group-name-prefix "/aws/apprunner/${APP_RUNNER_SERVICE_NAME}/" \
  --query "logGroups[].logGroupName" \
  --output text)

if [[ -z "${groups}" || "${groups}" == "None" ]]; then
  echo "No matching App Runner log groups found yet."
  echo "Deploy the service first, then run this script."
  exit 0
fi

for group in ${groups}; do
  echo "Setting retention to ${LOG_RETENTION_DAYS} days for ${group}"
  aws logs put-retention-policy \
    --region "${AWS_REGION}" \
    --log-group-name "${group}" \
    --retention-in-days "${LOG_RETENTION_DAYS}" >/dev/null
done

echo "Done."
