#!/bin/bash

SNAPSHOT="${NEXT_VERSION}-SNAPSHOT"

echo "Bumping version to ${SNAPSHOT}"

./mvnw --settings .build/maven-ci-settings.xml \
  --batch-mode --no-transfer-progress \
  versions:set -DnewVersion=${SNAPSHOT} -DgenerateBackupPoms=false

./mvnw --settings .build/maven-ci-settings.xml \
  --batch-mode --no-transfer-progress \
  versions:set -DnewVersion=${SNAPSHOT} -DgenerateBackupPoms=false \
  -pl bom
