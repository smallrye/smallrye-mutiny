#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

jbang .build/PreRelease.java --token=${GITHUB_TOKEN} --release-version=${RELEASE_VERSION}
