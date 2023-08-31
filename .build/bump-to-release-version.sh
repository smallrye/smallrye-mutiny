#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "Bumping version to ${RELEASE_VERSION}"

./mvnw --settings .build/maven-ci-settings.xml \
  --batch-mode --no-transfer-progress \
  versions:set -DnewVersion=${RELEASE_VERSION} -DgenerateBackupPoms=false

./mvnw --settings .build/maven-ci-settings.xml \
  --batch-mode --no-transfer-progress \
  versions:set -DnewVersion=${RELEASE_VERSION} -DgenerateBackupPoms=false \
  -pl bom

echo "Update website version to ${RELEASE_VERSION}"

.build/UpdateDocsAttributesFiles.java --mutiny-version=${RELEASE_VERSION}
