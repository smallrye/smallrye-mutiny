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

git fetch origin --tags
git reset --hard
git checkout "${REF}"
./mvnw -B clean deploy -DskipTests -Prelease -s maven-settings.xml

