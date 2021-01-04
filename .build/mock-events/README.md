This directory contains "mock" versions of Github events to test the release process:

The following command triggers a "dry run" of a release preparation:

```bash
act workflow_dispatch -e .build/mock-events/release-workflow-event.json \
    -s GITHUB_API_TOKEN=${GITHUB_TOKEN} \ 
    -s SECRET_FILES_PASSPHRASE="..." \
    -P ubuntu-latest=nektos/act-environments-ubuntu:18.04 \
    -r -a cescoffier
```

The following command triggers a tag creation event (which triggers the post-release, artifact deployment and web site update):

```bash
act create -e .build/mock-events/tag-creation-event.json \
    -s GITHUB_API_TOKEN=${GITHUB_TOKEN} \
    -s SECRET_FILES_PASSPHRASE="..." \
    -P ubuntu-latest=nektos/act-environments-ubuntu:18.04 \
    -r -a cescoffier
```
**IMPORTANT:** Comment the `deploy-release.sh` line in the `deploy-release.yaml` to avoid pushing artifacts to Maven central. 

