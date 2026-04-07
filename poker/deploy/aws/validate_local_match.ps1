param(
    [int]$PlayerCount = 2,
    [int]$MatchHands = 1
)

$ErrorActionPreference = "Stop"

Write-Host "Compiling Java sources..."
javac MainClass.java models/*.java game/*.java evaluation/*.java players/*.java ui/*.java | Out-Null

$inputs = @(
    "all-in",
    "",
    "",
    "",
    "",
    "",
    "n"
) -join "`n"

Write-Host "Running local smoke match..."
$output = ($inputs | java -cp ".;models;game;evaluation;players;ui" MainClass $PlayerCount $MatchHands | Out-String)

if ($output -notmatch "Match complete\." -or $output -notmatch "Play again\? \[y/n\]:") {
    Write-Error "Validation failed: expected end-of-match and replay prompt output."
}

Write-Host "Local validation passed."
