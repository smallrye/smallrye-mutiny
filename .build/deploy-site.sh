#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "🚧 Building..."

cd documentation
PROJECT_VERSION=$(yq '.attributes.versions.mutiny' attributes.yaml)

pipenv shell
mike deploy --push --update-aliases $PROJECT_VERSION latest
mike set-default --push latest

echo "🍺 Site updated!"
