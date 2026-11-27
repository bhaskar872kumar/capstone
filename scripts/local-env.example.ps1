# Local settings for Windows PowerShell.
# 1. Copy:   Copy-Item scripts\local-env.example.ps1 scripts\local-env.ps1   (local-env.ps1 is gitignored)
# 2. Fill in the values below.
# 3. Load:   . .\scripts\local-env.ps1   (in every new terminal before starting the backend)

$env:SPRING_PROFILES_ACTIVE = 'local'

# PostgreSQL role "healthdekho" and databases "healthdekho" + "healthdekho_test"
$env:DB_URL = 'jdbc:postgresql://localhost:5432/healthdekho'
$env:DB_USERNAME = 'healthdekho'
$env:DB_PASSWORD = 'change-me'
$env:TEST_DB_PASSWORD = $env:DB_PASSWORD

# Password for the three sample customers created by the seed migration (V2__seed_data.sql).
# Set it BEFORE the first start: it is hashed into the database on the first run. No quotes inside.
$env:DEMO_PASSWORD = 'change-me'

# Amazon Bedrock. Credentials come from `aws configure` (or set $env:AWS_PROFILE).
$env:AWS_REGION = 'us-east-1'
$env:BEDROCK_MODEL_ID = 'amazon.nova-lite-v1:0'
