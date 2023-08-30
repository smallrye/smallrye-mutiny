#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh
sdk install jbang
