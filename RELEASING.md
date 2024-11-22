# Releasing Mutiny
 
The release process is mostly done locally from a release branch.

## What you need

The `justfile` at the root of the repository contains tasks to help you in the release process.

Make sure that the following tools are installed and properly configured:
- [GitHub CLI (gh)](https://cli.github.com/)
- [yq](https://github.com/mikefarah/yq)
- [just](https://github.com/casey/just)
- [jbang](https://www.jbang.dev/)

We assume that anyone doing a release already has a proper Java runtime, Git and Maven tools installed 😇

## Prepare a release

Make sure that all issues and pull-requests assigned to the target milestone have been closed.

Call `just prepare-release` with the previous and release version, as in:

```
$ just prepare-release 2.6.2 2.7.0-RC5
```

This creates a local release branch called `release/{version}`.
- Follow the instructions to check the release notes and push the branch upstream (or else you won't be able to create a GitHub release).
- At this stage the branch is on the commit that set the release version.
- Run a quick check (`just install`) and also inspect the Git commits to make sure everything is fine.

## Perform a release

Once you are confident that the release can happen, run:

```
$ just perform-release
```

This creates a GitHub release and adds a few post-release commits on your release branch.
- The following GitHub Action workflows will be triggered by the new tag associated with the release:
  - the website will be deployed (unless this is a pre-release)
  - the Maven artifacts will be pushed to Maven Central.
- Make sure to wait for the Maven artifacts to be available on Maven Central.
- Once this is done, follow the instructions to merge the release branch, and push all your changes upstream.

