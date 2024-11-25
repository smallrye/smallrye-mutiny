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

### Conventional commits

Mutiny adopts [conventional commits](https://www.conventionalcommits.org/en/v1.0.0/) after the 2.4.0 release.

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
