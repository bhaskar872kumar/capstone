# Builds ONE runnable JAR with the React app inside: backend\target\healthdekho.jar
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$static = Join-Path $root 'backend\src\main\resources\static'

Write-Host '==> Building the React app'
Push-Location (Join-Path $root 'frontend')
npm install
if ($LASTEXITCODE -ne 0) { throw 'npm install failed' }
npm run build
if ($LASTEXITCODE -ne 0) { throw 'npm run build failed' }
Pop-Location

Write-Host '==> Copying the React build into the backend'
if (Test-Path $static) { Remove-Item $static -Recurse -Force }
New-Item -ItemType Directory -Path $static | Out-Null
Copy-Item (Join-Path $root 'frontend\dist\*') $static -Recurse

Write-Host '==> Building the Spring Boot JAR'
Push-Location (Join-Path $root 'backend')
if (Test-Path '.\mvnw.cmd') { .\mvnw.cmd -B -DskipTests package } else { mvn -B -DskipTests package }
if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
Pop-Location

Write-Host "Release JAR: $root\backend\target\healthdekho.jar"
