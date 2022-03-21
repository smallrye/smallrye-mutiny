# Contributing guide

**Want to contribute? Great!** 
We try to make it easy, and all contributions, even the smaller ones, are more than welcome.
This includes bug reports, fixes, documentation, examples... 
But first, read this page (including the small print at the end).

## Legal

All original contributions to SmallRye Mutiny are licensed under the
[ASL - Apache License](https://www.apache.org/licenses/LICENSE-2.0),
version 2.0 or later, or, if another license is specified as governing the file or directory being
modified, such other license.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/).
The DCO text is also included verbatim in the [dco.txt](dco.txt) file in the root directory of the repository.

## Reporting an issue

This project uses GitHub issues to manage the issues. 
Open an issue directly in [GitHub](https://github.com/smallrye/smallrye-mutiny/issues).

If you believe you found a bug, and it's likely possible, please indicate a way to reproduce it, what you are seeing and what you would expect to see.
Don't forget to indicate your SmallRye Mutiny, Java, and Maven/Gradle version. 

## Before you contribute

To contribute, use GitHub Pull Requests, from your **own** fork.

### Signing your Git commits

We require Git commits to be *[verified on GitHub](https://docs.github.com/en/authentication/managing-commit-signature-verification/about-commit-signature-verification)*, so please:
- [use a valid user name and email in your commits](https://docs.github.com/en/account-and-profile/setting-up-and-managing-your-github-user-account/managing-email-preferences/setting-your-commit-email-address), and
- [sign your commits](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits) with a GnuPG key that is tied to your GitHub account. 

### Code reviews

All non-trivial contributions, including contributions by project members, need to be reviewed before being merged.

### Continuous Integration

Because we are all humans, the project uses a continuous integration approach and each pull request triggers a full build.
Please make sure to monitor the output of the build and act accordingly.

Mutiny uses GitHub Actions as CI, so you can see the progress and results in the _checks_ tab of your pull request.
Follow the results on https://github.com/smallrye/smallrye-mutiny/actions.

### Tests and documentation are not optional

Don't forget to include tests in your pull requests. 
Also don't forget the documentation (reference documentation, javadoc...).

We even accept pull requests just containing tests or documentation.

## Setup

If you have not done so on this machine, you need to:
 
* Install Git and configure your GitHub access
* Install Java SDK (OpenJDK recommended, see https://adoptium.net/)
* Install Apache Maven (or use the `mvnw` wrapper scripts instead of `mvn`)

## Build

* Clone the repository: `git clone https://github.com/smallrye/smallrye-mutiny.git`
* Navigate to the directory: `cd smallrye-mutiny`
* Invoke `mvn clean install` from the root directory

```bash
git clone https://github.com/smallrye/smallrye-mutiny.git
cd smallrye-mutiny
mvn clean install
# Wait... success!
```

### Faster builds

Tests account for the majority of the build time.

There are 2 Maven profiles that you can activate to speed up the build of the Mutiny core library (in `implementation/`):

- `-Pskip-rs-tck` to avoid running the Reactive Streams TCK
- `-Pparallel-tests` to run the JUnit5 tests in parallel

The 2 profiles can be activated at the same time if you want to benefit from parallel tests and skip the Reactive Streams TCK.
This is mostly useful to have fast development feedback loops.

Note that parallel tests are not activated by default (yet) because some tests may randomly fail if your system is under load, or if it has constrained resources.
The Reactive Streams TCK is a good example as it uses some time-sensitive checks.

## The small print

This project is an open source project, please act responsibly, be nice, polite and enjoy!

## Deployment and Release

First you need an environment variable named `GITHUB_TOKEN` with a token allowing access to the GitHub API and having push permission.
Also, you need to check that:

* there are no build in progress of the `ref` branch (`main`)
* the last build of the `ref` branch (`main`) has been successful

Multiple steps compose the release process.

### Release preparation

The "prepare-release" workflow verifies that a release can be done. 
Typically, it computes the target version if not set, and verifies that:

1. a milestone with the release version as name exists and as no more open issues
2. a tag with the release version as name does not already exist

Then, it bumps the version, builds the project and pushed a tag.
Finally, it clears the breaking change justifications.

You can trigger a release using a _workflow dispatch_ events or directly from the Github Actions page.
Parameters are the following:

- `dry-run` - if `true` to not push anything to the remote git repository (default: `false`).
- `release-version` - the target release version. If not set, it computes the release version by bumping the _minor_ version digit, or the _micro_ version digit is `micro` is set to `true`.
The last version is the last pushed tag.
- `micro` - when the `release-version` is not set, indicates that the version is a _micro_ (default: `false`).
- `skip-tests` - if `true`, skips the tests during the project build (default: `false`)
- `branch` - the branch from which the release must be cut (default: `main`)

Check https://github.com/smallrye/smallrye-mutiny/actions to follow the progress.

The workflow triggers the other steps automatically (upon the tag creation).

### Web Site deployment

When the "prepare-release" workflows pushes the tag, the "deploy-site" workflows starts (upon the tag creation event), and builds the website from the tag.
It pushes the website, so once completed, the website is up to date.

### Artifact deployment to Maven Central

When the "prepare-release" workflows pushes the tag, the "deploy-release" workflows starts (upon the tag creation event), and builds the artifacts from the tag.
It also deploys them to Maven Central.

### Post-Release

When the "prepare-release" workflows pushes the tag, the "post-release" workflows starts (upon the tag creation event), and creates the Github Release.
It computes the changelog, collects the breaking changes and creates the release (if it does not exist).
It also creates the next milestone (if it does not exist yet) and closes the previous one.

The next milestone name is the current version with an increment to the minor version digit.

The "post-release" workflow is idempotent.

### Running the release process locally.

It is possible to runs the release process locally using [https://github.com/nektos/act](https://github.com/nektos/act).
In addition to `act`, you need a Github Token with push permission to the repository (stored in the `GITHUB_TOKEN` env variable), and the SmallRye GPG Key passphrase (stored in the `PASSPHRASE` env variable).

Then, edit the `.build/mock-events/release-workflow-event.json` to adapt the execution:

```json
{
  "inputs": {
    "skip-tests": true,
    "branch": "main",
    "release-version": "0.12.5"
  }
}
``` 

Then, from the project root, runs:

```bash
act workflow_dispatch -e .build/mock-events/release-workflow-event.json \
    -s GITHUB_API_TOKEN=${GITHUB_TOKEN} \ 
    -s SECRET_FILES_PASSPHRASE=${PASSPHRASE} \
    -P ubuntu-latest=nektos/act-environments-ubuntu:18.04 \
    -r -a YOUR_GITHUB_NAME
```
 
This would execute the release preparation. 
Once completed, because it creates the tag, the other steps will start.

If you need to run the other steps manually, edit the `.build/mock-events/tag-creation-event.json` to adapt it with the target tag.
Then, runs:

```bash
act push -e .build/mock-events/tag-creation-event.json \
    -s GITHUB_API_TOKEN=${GITHUB_TOKEN} \ 
    -s SECRET_FILES_PASSPHRASE=${PASSPHRASE} \
    -P ubuntu-latest=nektos/act-environments-ubuntu:18.04 \
    -r -a YOUR_GITHUB_NAME
```




