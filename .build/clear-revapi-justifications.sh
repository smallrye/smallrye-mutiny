#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "Clearing difference justifications"

source ~/.sdkman/bin/sdkman-init.sh
jbang .build/CompatibilityUtils.java clear --version="${RELEASE_VERSION}" --do-not-clear-version-prefix="1."

if [[ $(git diff --stat) != '' ]]; then
  git add -A
  git status
  git commit -m "Clearing breaking change justifications"
  git push
else
  echo "No justifications cleared"
fi
