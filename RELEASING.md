# Releasing Mutiny

## Prepare a release pull-request 

1. Make sure that all issues and pull-requests assigned to the target milestone have been closed.
2. Run: `just prepare-release 2.7.0-RC3` (replace with the correct version number).
3. Review the changes in the branch.
4. Open a pull-request: `gh pr create`.
5. Once all tests pass, merge it:
    - the `pre-release.yml` workflow will create a Git tag, then
    - the `release.yml` workflow will push to Maven Central, then
    - the `deploy-site.yml` workflow will update the website (except for release candidates and milestones).

## GitHub release notes

Run the following command: `just jreleaser 2.6.2 2.7.0-RC3` where you specify the previous and the release tags to compute release notes.

If the release notes fail to update or if they are too big for JReleaser to update them, then you can read them from `target/jreleaser/release/CHANGELOG.md` and manually update the release notes on GitHub.
