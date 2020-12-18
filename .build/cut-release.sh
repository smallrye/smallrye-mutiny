#!/usr/bin/env bash
set -e

init_gpg() {
    gpg2 --fast-import --no-tty --batch --yes smallrye-sign.asc
}

init_git() {
    git config --global user.name "${GITHUB_ACTOR}"
    git config --global user.email "smallrye@googlegroups.com"

    git update-index --assume-unchanged .build/deploy.sh
    git update-index --assume-unchanged .build/decrypt-secrets.sh
}

# -------- SCRIPT START HERE -----------------

init_git
init_gpg

export VERSION=""
export BRANCH="HEAD"
export EXTRA_PRE_ARGS=""

if [[ ${MICRO_RELEASE} == "true" ]]; then
  EXTRA_PRE_ARGS="--micro"
fi

if [[ ! -z "${USER_NAME}" ]]; then
  EXTRA_PRE_ARGS="${EXTRA_PRE_ARGS} --username=${USER_NAME}"
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

echo "Cutting release ${VERSION}"
mvn -B -fn clean
git checkout ${BRANCH}
HASH=$(git rev-parse --verify $BRANCH)
echo "Last commit is ${HASH} - creating detached branch"
git checkout -b "r${VERSION}" "${HASH}"

echo "Update version to ${VERSION}"
mvn -B versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false -s maven-settings.xml

if [[ ${SKIP_TESTS} == "true" ]]; then
  mvn -B clean verify -Prelease -DskipTests -s maven-settings.xml
else
  mvn -B clean verify -Prelease -s maven-settings.xml
fi

echo "Update website version to ${VERSION}"
sed -ie "s/mutiny_version: .*/mutiny_version: ${VERSION}/g" documentation/src/main/jekyll/_data/versions.yml

git commit -am "[RELEASE] - Bump version to ${VERSION}"
git tag "${VERSION}"
echo "Pushing tag to origin"
# git push origin "${VERSION}"