#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

curl -Ls https://sh.jbang.dev | bash -s - app setup
source ~/.bashrc
