#!/usr/bin/env bash
set -e

init_gpg() {
    gpg2 --fast-import --no-tty --batch --yes smallrye-sign.asc
}

init_git() {
    git config --global user.name "${GITHUB_ACTOR}"
    git config --global user.email "smallrye@googlegroups.com"
}

# -------- SCRIPT START HERE -----------------

init_git
init_gpg

export VERSION=""
export BRANCH="HEAD"
export EXTRA_PRE_ARGS=""

if [[ ${DRY_RUN} == "true" ]]; then
  echo "[DRY RUN] - Dry Run Enabled - Git push will be skipped."
fi

if [[ ${MICRO_RELEASE} == "true" ]]; then
  EXTRA_PRE_ARGS="--micro"
fi

if [[ ! -z "${RELEASE_VERSION}" ]]; then
  EXTRA_PRE_ARGS="${EXTRA_PRE_ARGS} --release-version=${RELEASE_VERSION}"
fi

jbang .build/PreRelease.java --token="${GITHUB_TOKEN}" ${EXTRA_PRE_ARGS}

export VERSION=""
if [ -f /tmp/release-version ]; then
  VERSION=$(cat /tmp/release-version)
else
    echo "'/tmp/release-version' expected after pre-release"
    exit 1
fi

echo "Update website version to ${VERSION}"
.build/UpdateDocsAttributesFiles.java --mutiny-version=${VERSION}
if [[ $(git diff --stat) != '' ]]; then
    git add documentation/attributes.yaml
    git commit -m "Bumping the website version to ${VERSION}"
    git push
    echo "Version updated"
else
  echo "The version was already correct"
fi

echo "Cutting release ${VERSION}"
./mvnw -s .build/maven-ci-settings.xml -B -fn clean
git checkout ${BRANCH}
HASH=$(git rev-parse --verify $BRANCH)
echo "Last commit is ${HASH} - creating detached branch"
git checkout -b "r${VERSION}" "${HASH}"

echo "Update version to ${VERSION}"
./mvnw -B versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false -s maven-settings.xml
./mvnw -B versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false -s maven-settings.xml -pl bom

if [[ ${SKIP_TESTS} == "true" ]]; then
  ./mvnw -B clean verify -Prelease -DskipTests -s maven-settings.xml
else
  ./mvnw -B clean verify -Prelease -s maven-settings.xml
fi

git commit -am "[RELEASE] - Bump version to ${VERSION}"
git tag "${VERSION}"
echo "Pushing tag to origin"
if [[ ${DRY_RUN} == "true" ]]; then
  echo "[DRY RUN] - Skipping push: git push origin ${VERSION}"
else
  git push origin "${VERSION}"
fi