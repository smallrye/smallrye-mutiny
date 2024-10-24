#!/usr/bin/env just --justfile

# Do a quick build
quick-build:
    ./mvnw -Dquickly

# Run all the tests
verify:
    ./mvnw verify -Pparallel-tests -T8

# Prepare a release pull-request branch
prepare-release version:
    @echo "🚀 Preparing a pull-request branch for releasing version {{version}}"
    git switch -c release/{{version}}
    yq -i '.release.current-version = "{{version}}"'  .github/project.yml
    jbang .build/UpdateDocsAttributesFiles.java --mutiny-version={{version}}
    ./mvnw --batch-mode --no-transfer-progress -Pupdate-workshop-examples -f workshop-examples compile -DworkshopVersion={{version}}
    find workshop-examples -name '*.java' | xargs chmod +x
    git commit -am "chore(release): update metadata for Mutiny {{version}}"
    just clear-revapi
    @echo "✅ All set, please review changes then open a pull-request from this branch!"

# Use JReleaser to generate a changelog and announce a release
jreleaser previousReleaseTag releaseTag:
    #!/usr/bin/env bash
    echo "🚀 Use JReleaser for the release of {{previousReleaseTag}} to {{releaseTag}}"
    export CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
    export JRELEASER_GITHUB_TOKEN=$(gh auth token)
    export JRELEASER_PROJECT_VERSION={{releaseTag}}
    export JRELEASER_TAG_NAME={{releaseTag}}
    export JRELEASER_PREVIOUS_TAG_NAME={{previousReleaseTag}}
    echo "💡 Checking out tag {{releaseTag}}"
    git checkout {{releaseTag}}
    ./mvnw --batch-mode --no-transfer-progress -Pjreleaser jreleaser:full-release -pl :mutiny-project
    echo "💡 Back to branch ${CURRENT_BRANCH}"
    git checkout ${CURRENT_BRANCH}
    echo "✅ JReleaser completed"

# Clear RevAPI justifications
clear-revapi:
    #!/usr/bin/env bash
    jbang .build/CompatibilityUtils.java clear --version="${RELEASE_VERSION}" --do-not-clear-version-prefix="1."
    if [[ $(git diff --stat) != '' ]]; then
      git add -A
      git status
      git commit -m "chore(release): clear RevAPI breaking change justifications"
      git push
    else
      echo "No justifications cleared"
    fi