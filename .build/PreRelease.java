///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.kohsuke:github-api:1.117
//DEPS info.picocli:picocli:4.5.0
//SOURCES Helper.java

import org.kohsuke.github.*;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;

import static helpers.Helper.*;

/**
 * Script checking that the repository can be released.
 * It checks that:
 * - the new version target an existing milestone
 * - the milestone has no opened issues
 * - the tag does not exist
 *
 * Run with `./PreRelease.java --token=GITHUB_TOKEN [--micro] [--release-version=version]
 */
@CommandLine.Command(name = "pre-release", mixinStandardHelpOptions = true, version = "0.1",
        description = "Pre-Release Check")
public class PreRelease implements Callable<Integer> {

    @CommandLine.Option(names = "--token", description = "The Github Token", required = true)
    private String token;

    @CommandLine.Option(names = "--micro", description = "To set the release to be a micro release", defaultValue = "false")
    private boolean micro;

    @CommandLine.Option(names = "--release-version", description = "Set the released version - if not set, the version is computed")
    private String target;

    private static final String REPO = "smallrye/smallrye-mutiny";

    public static void main(String... args) {
        int exitCode = new CommandLine(new PreRelease()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (micro) {
            info("Preparing micro release");
        } else {
            info("Preparing release");
        }

        GitHub gitHub = new GitHubBuilder().withOAuthToken(token).build();
        GHRepository repository = gitHub.getRepository(REPO);

        List<GHTag> tags = repository.listTags().toList();
        List<GHMilestone> milestones = repository.listMilestones(GHIssueState.ALL).toList();

        String newVersion;
        if (target != null  && ! target.trim().isEmpty()) {
            newVersion = target;
        } else {
            info("No version passed, computing new version from the last tag");
            GHTag tag = getLastTag(tags);
            newVersion = computeNextVersion(repository, tag, micro);
        }
        info("Released version would be %s", newVersion);

        info("Running pre-checks...");
        failIfTrue(milestones::isEmpty, "No milestones in repository %s", repository.getName());

        // Check that the is no tag with the name newVersion
        if (tags.stream().anyMatch(t -> t.getName().equals(newVersion))) {
            fail("Cannot cut release %s - there is already a tag with this version", newVersion);
        }
        success("No existing tag named %s", newVersion);

        // Check that there is a milestone named newVersion
        GHMilestone milestone = milestones.stream().filter(m -> m.getTitle().equals(newVersion)).findFirst().orElse(null);
        if (milestone == null) {
            fail("No milestone named %s in repository %s - you need to create one", newVersion, repository.getName());
        } else {
            success("Milestone %s found with %d closed issues",
                    milestone.getTitle(),
                    milestone.getClosedIssues());
        }

        assert milestone != null;
        // Check that all the issues associated with the milestone are closed
        failIfTrue(() -> milestone.getOpenIssues() != 0, "The milestone %s has %d opened issues, all the issues "
                + "must be closed or moved to another milestone before being able to cut the release. Visit %s for details",
                milestone.getTitle(), milestone.getOpenIssues(), milestone.getHtmlUrl().toExternalForm());
        success("No remaining (open) issue associated with the milestone %s", milestone.getTitle());

        success("Release pre-checks successful!");
        info("Writing release version in the /tmp/release-version file");
        Files.write(new File("/tmp/release-version").toPath(), newVersion.getBytes());
        if (micro) {
            //noinspection ResultOfMethodCallIgnored
            new File("/tmp/micro").createNewFile();
        }

        completed("Pre-release checks completed!");

        return 0;
    }

    private GHTag getLastTag(List<GHTag> tags) {
        failIfTrue(tags::isEmpty, "No tags found in repository");
        GHTag tag = first(tags);
        assert tag != null;
        success("Last tag retrieved: %s", tag.getName());
        return tag;
    }

}
