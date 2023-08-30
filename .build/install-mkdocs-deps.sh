#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

cd documentation
pip install pipenv
pipenv install
