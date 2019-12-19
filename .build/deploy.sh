#!/usr/bin/env bash
set -e

export TARGET=$1

init_mvn() {
    echo "settings.xml created:"
    ls -l | grep maven-settings
}

init_gpg() {
    echo "signature file imported:"
    ls -l | grep smallrye-sign
    gpg2 --fast-import --no-tty --batch --yes smallrye-sign.asc
}

init_git() {
    git config --global user.name "${GITHUB_ACTOR}"
    git config --global user.email "smallrye@googlegroups.com"
}

deploy_snapshot() {
    echo "Deploying snapshot"
    mvn -B -fn clean
    mvn -B deploy -DskipTests -s maven-settings.xml
}

deploy_release() {
    export RELEASE_VERSION=${MAJOR}.${MINOR}.${MICRO}

    export NEXT_VERSION=${MAJOR}.$(expr ${MINOR} + 1).${MICRO}-SNAPSHOT
    if [[ ${MICRO_RELEASE} == "true" ]]; then
      NEXT_VERSION=${MAJOR}.${MINOR}.$(expr ${MICRO} + 1)-SNAPSHOT
    fi

    echo "Cutting release ${RELEASE_VERSION}, next version would be ${NEXT_VERSION}"
    mvn -B -fn clean
    mvn -B versions:set -DnewVersion=${RELEASE_VERSION} -DgenerateBackupPoms=false -s maven-settings.xml
    mvn -B clean install -DskipTests -Prelease -s maven-settings.xml
    git commit -am "[RELEASE] - Bump version to ${RELEASE_VERSION}"
    git tag "${RELEASE_VERSION}"
    echo "Pushing tag to origin"
    git push origin "${RELEASE_VERSION}"
    mvn -B versions:set -DnewVersion=${NEXT_VERSION} -DgenerateBackupPoms=false -s maven-settings.xml
    git commit -am "[RELEASE] - Bump version to ${NEXT_VERSION}"
    git push origin master
    git checkout ${RELEASE_VERSION}
    mvn -B deploy -DskipTests -Prelease -s maven-settings.xml
}

init_git
init_gpg
init_mvn

export VERSION=$(mvn help:evaluate -Dexpression=project.version|grep '^[0-9]*\.[0-9]*\.[0-9]*-SNAPSHOT'|sed 's/-SNAPSHOT//')
echo "Current version of the project is ${VERSION}"

export MAJOR=$(echo ${VERSION}|tr '.' ' '|awk '{print $1}')
export MINOR=$(echo ${VERSION}|tr '.' ' '|awk '{print $2}')
export MICRO=$(echo ${VERSION}|tr '.' ' '|awk '{print $3}')

git update-index --assume-unchanged .build/deploy.sh
git update-index --assume-unchanged .build/decrypt-secrets.sh

export EXTRA_ARGS=""
if [[ ${MICRO_RELEASE} == "true" ]]; then
  EXTRA_ARGS="--micro"
fi

if [[ ${TARGET} == "snapshot" ]]; then
    deploy_snapshot
elif [[ ${TARGET} == "release" ]]; then
    echo "Checking release prerequisites"
    .build/prepare-release.kts "${GITHUB_TOKEN}" "${EXTRA_ARGS}"

    echo "Cutting release"
    deploy_release

    echo "Updating documentation"
    .build/doc.sh

    echo "Executing post-release"
    .build/post-release.kts "${GITHUB_TOKEN}"
else
    echo "Unknown environment: ${TARGET}"
fi
