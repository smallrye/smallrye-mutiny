#!/usr/bin/env just --justfile

set shell := ["bash", "-uc"]

# Do a quick build
quick-build:
    ./mvnw -Dquickly

# Maven install
install:
    ./mvnw clean install


# Run all the tests
verify:
    ./mvnw verify -Pparallel-tests -T8

# Prepare a release branch
prepare-release previousVersion version:
    @echo "🚀 Preparing a branch for releasing version {{version}}"
    git switch -c release/{{version}}
    yq -i '.release.current-version = "{{version}}"' .github/project.yml
    yq -i '.release.previous-version = "{{previousVersion}}"' .github/project.yml
    ./mvnw --batch-mode --no-transfer-progress versions:set -DnewVersion={{version}} -DgenerateBackupPoms=false
    jbang .build/UpdateDocsAttributesFiles.java --mutiny-version={{version}}
    ./mvnw --batch-mode --no-transfer-progress clean install -DskipTests
    ./mvnw --batch-mode --no-transfer-progress -Pupdate-workshop-examples -f workshop-examples compile -DworkshopVersion={{version}}
    find workshop-examples -name '*.java' | xargs chmod +x
    git commit -am "chore(release): update version metadata for Mutiny {{version}}"
    just changelog
    @echo "✅ All set, please review the changes on this branch before doing the release, then:"
    @echo "   - git push origin release/{{version}} --set-upstream"
    @echo "   - just perform-release"

# Compute a changelog
changelog:
    #!/usr/bin/env bash
    export PREVIOUS_VERSION=$(yq '.release.previous-version' .github/project.yml)
    export RELEASE_VERSION=$(yq '.release.current-version' .github/project.yml)
    export NEXT_VERSION=$(yq '.release.next-version' .github/project.yml)
    export JRELEASER_GITHUB_TOKEN=$(gh auth token)
    export JRELEASER_PROJECT_VERSION=${RELEASE_VERSION}
    export JRELEASER_TAG_NAME=${RELEASE_VERSION}
    export JRELEASER_PREVIOUS_TAG_NAME=${PREVIOUS_VERSION}
    ./mvnw --batch-mode --no-transfer-progress -Pjreleaser jreleaser:changelog -pl :mutiny-project
    echo "✅ Release notes ok, check target/jreleaser/release/CHANGELOG.md"

# Perform a release
perform-release:
    #!/usr/bin/env bash
    just changelog
    export PREVIOUS_VERSION=$(yq '.release.previous-version' .github/project.yml)
    export RELEASE_VERSION=$(yq '.release.current-version' .github/project.yml)
    export NEXT_VERSION=$(yq '.release.next-version' .github/project.yml)
    export RELEASE_BRANCH="release/${RELEASE_VERSION}"
    echo "🚀 Releasing: ${PREVIOUS_VERSION} ➡️ ${RELEASE_VERSION} ➡️ ${NEXT_VERSION}"
    pre_release=0
    gh_extra_args=""
    case "${RELEASE_VERSION}" in
      *-RC*)
        pre_release=1
        gh_extra_args="${gh_extra_args} --prerelease --latest=false"
        ;;
      *-M*)
        pre_release=1
        gh_extra_args="${gh_extra_args} --prerelease --latest=false"
        ;;
    esac
    if [[ pre_release -eq 1 ]]; then
      echo '🧪 This is a pre-release'
    fi
    gh release create ${RELEASE_VERSION} \
      --discussion-category 'Announcements' \
      --notes-file target/jreleaser/release/CHANGELOG.md \
      --target ${RELEASE_BRANCH} \
      ${gh_extra_args}
    echo "✅ Release created"
    ./mvnw --batch-mode --no-transfer-progress versions:set -DnewVersion=${NEXT_VERSION} -DgenerateBackupPoms=false
    git commit -am "chore(release): set development version to ${NEXT_VERSION}"
    if [[ pre_release -eq 0 ]]; then
      just clear-revapi
    else
      echo "💡 We don't clear RevAPI justifications on a pre-release"
    fi
    echo "✅ All set, don't forget to merge this branch and push upstream once Maven artifacts are available"
    echo "💡 If you released from main:"
    echo "      git switch main"
    echo "      git merge ${RELEASE_BRANCH}"
    echo "      git push"

# Clear RevAPI justifications
clear-revapi:
    #!/usr/bin/env bash
    jbang .build/CompatibilityUtils.java clear --version="${RELEASE_VERSION}" --do-not-clear-version-prefix="1."
    if [[ $(git diff --stat) != '' ]]; then
      git add -A
      git status
      git commit -m "chore(release): clear RevAPI breaking change justifications"
    else
      echo "No justifications cleared"
    fi