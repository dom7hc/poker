param(
    [Parameter(Mandatory = $true)]
    [string]$ServiceUrl,

    [string]$WebCredentials = ""
)

$ErrorActionPreference = "Stop"

if (-not $ServiceUrl.StartsWith("http")) {
    $ServiceUrl = "https://$ServiceUrl"
}

Write-Host "Checking service URL: $ServiceUrl"

$headers = @{}
if ($WebCredentials -ne "") {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($WebCredentials)
    $headers["Authorization"] = "Basic " + [System.Convert]::ToBase64String($bytes)
}

$response = Invoke-WebRequest -Uri $ServiceUrl -Headers $headers -UseBasicParsing

if ($response.StatusCode -ne 200) {
    Write-Error "Unexpected HTTP status: $($response.StatusCode)"
}

if ($response.Content -notmatch "ttyd" -and $response.Content -notmatch "xterm") {
    Write-Warning "Response did not match expected terminal markers. Verify manually in browser."
} else {
    Write-Host "Website validation passed (terminal page detected)."
}
