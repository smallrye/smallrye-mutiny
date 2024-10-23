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