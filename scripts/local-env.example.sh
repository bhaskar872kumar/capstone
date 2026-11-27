# Local settings for mac/Linux.
# 1. Copy:   cp scripts/local-env.example.sh scripts/local-env.sh   (local-env.sh is gitignored)
# 2. Fill in the values below.
# 3. Load:   source scripts/local-env.sh   (in every new terminal before starting the backend)

export SPRING_PROFILES_ACTIVE=local

# PostgreSQL role "healthdekho" and databases "healthdekho" + "healthdekho_test"
export DB_URL=jdbc:postgresql://localhost:5432/healthdekho
export DB_USERNAME=healthdekho
export DB_PASSWORD=change-me
export TEST_DB_PASSWORD="$DB_PASSWORD"

# Password for the three sample customers created by the seed migration (V2__seed_data.sql).
# Set it BEFORE the first start: it is hashed into the database on the first run. No quotes inside.
export DEMO_PASSWORD=change-me

# Amazon Bedrock. Credentials come from `aws configure` (or set AWS_PROFILE=your-profile).
export AWS_REGION=us-east-1
export BEDROCK_MODEL_ID=amazon.nova-lite-v1:0
