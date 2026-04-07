# Deploy Console Poker to AWS App Runner

This deploy path publishes the existing console game as a website by running it behind a browser terminal (`ttyd`) in a container.

## What You Get

- HTTPS URL hosted by App Runner
- Same interactive experience as local console game
- Configurable player count and hands per match through runtime env vars
- Optional web terminal basic auth (`WEB_CREDENTIALS`)

## Prerequisites

- AWS CLI configured for your sandbox account
- Docker installed locally
- IAM permissions for ECR, App Runner, IAM role creation, CloudWatch Logs

## Files

- `poker/Dockerfile`
- `poker/start.sh`
- `poker/deploy/aws/apprunner.env.example`
- `poker/deploy/aws/deploy_apprunner.sh`
- `poker/deploy/aws/set_log_retention.sh`
- `poker/deploy/aws/SECURITY.md`
- `poker/deploy/aws/validate_local_match.ps1`
- `poker/deploy/aws/validate_service_url.ps1`

## First Deploy

1. Copy env template:

```bash
cp deploy/aws/apprunner.env.example deploy/aws/apprunner.env
```

2. Edit `deploy/aws/apprunner.env` and set:

- `AWS_REGION`
- `ECR_REPOSITORY`
- `APP_RUNNER_SERVICE_NAME`
- `WEB_CREDENTIALS` (recommended in sandbox)

3. Run deploy script from the `poker/` directory:

```bash
bash deploy/aws/deploy_apprunner.sh
```

The script will:

1. Build container image
2. Push image to ECR
3. Create/update App Runner service
4. Print the service URL

## Security Guardrails (Sandbox)

Default hardening in this repo:

- Deploy script requires `WEB_CREDENTIALS` unless you explicitly set `ALLOW_NO_AUTH=true`
- Container startup script refuses to run without credentials unless `ALLOW_NO_AUTH=true`

Recommended sandbox controls:

- Use strong credentials in `WEB_CREDENTIALS`
- Keep App Runner instance count low
- Put CloudFront + WAF in front if you need IP allowlists

## Logging Defaults

After first deploy, apply CloudWatch retention:

```bash
bash deploy/aws/set_log_retention.sh
```

Default retention is `14` days (`LOG_RETENTION_DAYS` in env file).

## Update Deploy

Re-run the same script after code changes:

```bash
bash deploy/aws/deploy_apprunner.sh
```

## Rollback

Re-run deploy script using an older `IMAGE_TAG`:

```bash
IMAGE_TAG=20260406123000 bash deploy/aws/deploy_apprunner.sh
```

## Validation

### Local match flow (pre-deploy)

```powershell
powershell -ExecutionPolicy Bypass -File deploy/aws/validate_local_match.ps1
```

### Website check (post-deploy)

```powershell
powershell -ExecutionPolicy Bypass -File deploy/aws/validate_service_url.ps1 `
  -ServiceUrl "<apprunner-url>" `
  -WebCredentials "username:password"
```

## Expected Runtime Behavior

- Browser users see the ASCII table and prompts exactly like terminal
- Match and replay loop work as they do locally
- One container = one game process; this is not multi-session game server yet

## Known Limitations

- This is terminal-over-web, not a true browser game API yet
- Session persistence/user profiles are not implemented
- Multi-player browser concurrency is not isolated by match/session

## Angular Next Step

For the API/WebSocket bridge design, see:

- `poker/docs/angular-bridge.md`
