# AWS Sandbox Security Checklist

This checklist is intended for the phase-1 terminal website deployment.

## Baseline Controls in This Repo

- Web terminal auth is required by default (`WEB_CREDENTIALS`)
- Deploy script blocks deploys without credentials unless `ALLOW_NO_AUTH=true`
- Container startup blocks boot without credentials unless `ALLOW_NO_AUTH=true`
- CloudWatch log retention script is included (`set_log_retention.sh`)

## Recommended AWS Controls

1. Keep deployment in sandbox account only.
2. Rotate `WEB_CREDENTIALS` regularly.
3. Restrict who can run `deploy_apprunner.sh` (least privilege IAM).
4. If public URL is required, place CloudFront + WAF in front for IP allowlisting.
5. Set CloudWatch retention to 14-30 days.
6. Configure billing alarms for App Runner and ECR.

## Minimum IAM Permissions Needed for Deployment Script

- `ecr:*` (scoped to one repository where possible)
- `apprunner:*` (scoped to one service where possible)
- `iam:GetRole`, `iam:CreateRole`, `iam:AttachRolePolicy`, `iam:PassRole`
- `sts:GetCallerIdentity`
- `logs:DescribeLogGroups`, `logs:PutRetentionPolicy`
