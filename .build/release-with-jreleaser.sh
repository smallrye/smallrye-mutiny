#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "📦 Staging artifacts..."

./mvnw --settings .build/maven-ci-settings.xml \
  --batch-mode --no-transfer-progress \
  -Pjreleaser-staging \
  -DskipTests=true -Drevapi.skip=true

echo "🚀 Releasing..."

./mvnw --settings .build/maven-ci-settings.xml \
  --batch-mode --no-transfer-progress \
  -pl :mutiny-project -Pjreleaser-release \
  jreleaser:full-release -DskipTests=true -Drevapi.skip=true

echo "🎉 Done!"