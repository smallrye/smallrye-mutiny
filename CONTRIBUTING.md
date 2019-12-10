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

### Code reviews

All non-trivial contributions, including contributions by project members, need to be reviewed before being merged.

### Continuous Integration

Because we are all humans, the project uses a continuous integration approach and each pull request triggers a full build.
Please make sure to monitor the output of the build and act accordingly.

Mutiny uses GitHub Actions as CI, so you can see the progress and results in the _checks_ tab of your pull request.

### Tests and documentation are not optional

Don't forget to include tests in your pull requests. 
Also don't forget the documentation (reference documentation, javadoc...).

We even accept pull requests just containing tests or documentation.

## Setup

If you have not done so on this machine, you need to:
 
* Install Git and configure your GitHub access
* Install Java SDK (OpenJDK recommended)
* Install Apache Maven

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

## The small print

This project is an open source project, please act responsibly, be nice, polite and enjoy!
