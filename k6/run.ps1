param(
    [ValidateSet('smoke', 'load', 'stress', 'spike')]
    [string]$Test = 'smoke',
    [string]$BaseUrl = 'http://host.docker.internal:8085'
)

# Runs a k6 script with the official Docker image (no local k6 install needed).
# Usage: .\k6\run.ps1 -Test load
$root = Split-Path -Parent $PSScriptRoot

docker run --rm -i `
    --add-host=host.docker.internal:host-gateway `
    -v "${root}/k6:/scripts:ro" `
    -e BASE_URL=$BaseUrl `
    grafana/k6:latest run "/scripts/$Test.js"
