#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "🚧 Building..."

cd documentation
PROJECT_VERSION=$(yq '.attributes.versions.mutiny' attributes.yaml)

pipenv run mike deploy --push --update-aliases $PROJECT_VERSION latest
pipenv run mike set-default --push latest

echo "🍺 Site updated!"
