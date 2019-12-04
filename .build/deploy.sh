#!/usr/bin/env bash
set -e

export TARGET=$1

init_mvn() {
    echo ${MAVEN_SETTINGS} > maven-settings.xml
    echo "settings.xml created:"
    ls -l | grep maven-settings
}

init_gpg() {
    echo ${SMALLRYE_SIGNATURE} > smallrye-sign.asc
    ls -l | grep smallrye-sign
    echo "signature file imported:"
    gpg --fast-import smallrye-sign.asc
}

init_git() {
    git config --global user.name "SmallRye Continuous Builder"
    git config --global user.email "smallrye@googlegroups.com"
}

deploy_snapshot() {
    echo "Deploying snapshot"
    mvn -B -fn clean
    mvn -B deploy -DskipTests -s maven-settings.xml
}

deploy_release() {
    export RELEASE_VERSION=${MAJOR}.${MINOR}.${MICRO}
    export NEXT_VERSION=${MAJOR}.${MINOR}.$(expr ${MICRO} + 1)-SNAPSHOT
    echo "Cutting release ${RELEASE_VERSION}, next version would be ${NEXT_VERSION}"
    mvn -B -fn clean
    mvn -B -s maven-settings.xml release:prepare -DskipTests -Prelease -DgenerateBackupPoms=false -DreleaseVersion=${RELEASE_VERSION} -DdevelopmentVersion=${NEXT_VERSION}
    mvn -B -s maven-settings.xml release:perform -DskipTests -Prelease -Darguments=-DskipTests
}

init_git
init_gpg
init_mvn

export VERSION=$(mvn help:evaluate -Dexpression=project.version|grep '^[0-9]*\.[0-9]*\.[0-9]*-SNAPSHOT'|sed 's/-SNAPSHOT//')
echo "Current version of the project is ${VERSION}"

export MAJOR=$(echo ${VERSION}|tr '.' ' '|awk '{print $1}')
export MINOR=$(echo ${VERSION}|tr '.' ' '|awk '{print $2}')
export MICRO=$(echo ${VERSION}|tr '.' ' '|awk '{print $3}')


if [[ ${TARGET} == "snapshot" ]]; then
    deploy_snapshot
else
    release
fi

echo "DONE"


