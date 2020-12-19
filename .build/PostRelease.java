///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.kohsuke:github-api:1.117
//DEPS info.picocli:picocli:4.5.0
//SOURCES Helper.java

import org.kohsuke.github.*;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static helpers.Helper.*;

/**
 * Script run after the release.
 * The script does the following action in this order:
 * - checks that the previous milestone is closed, close it if not
 * - checks that the next milestone exists, or create it if not
 * - creates the Github release and compute the release notes
 * <p>
 * Run with `./PostRelease.java --token=GITHUB_TOKEN --release-version=version
 * <p>
 * 1. The github token is mandatory.
 * <p>
 * The version is taken from the last tag if not set.
 */
@CommandLine.Command(name = "post-release", mixinStandardHelpOptions = true, version = "0.1",
        description = "Post-Release Check")
public class PostRelease implements Callable<Integer> {

    @CommandLine.Option(names = "--token", description = "The Github Token", required = true)
    private String token;

    @CommandLine.Option(names = "--release-version", description = "Set the released version", required = true)
    private String releaseVersion;

    private static final String REPO = "smallrye/smallrye-mutiny";

    public static void main(String... args) {
        int exitCode = new CommandLine(new PostRelease()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        GitHub gitHub = new GitHubBuilder().withOAuthToken(token).build();
        GHRepository repository = gitHub.getRepository(REPO);

        List<GHTag> tags = repository.listTags().toList();
        List<GHMilestone> milestones = repository.listMilestones(GHIssueState.ALL).toList();

        info("Running post-release checks for release %s", releaseVersion);

        // Check that the tag associated with the release version exists
        GHTag tag = getTag(releaseVersion, tags);

        assert tag != null;

        // Check that the milestone exists (this check has already been done during the pre-release checks, just to be double sure)
        GHMilestone milestone = milestones.stream().filter(m -> m.getTitle().equals(releaseVersion)).findFirst()
                .orElse(null);
        failIfTrue(() -> milestone == null, "Unable to find the milestone %s", releaseVersion);
        assert milestone != null;
        success("Milestone %s found (%s)", milestone.getTitle(), milestone.getHtmlUrl());

        success("Post-release check successful");
        info("Starting post-release tasks");

        // Close milestone
        if (milestone.getState() != GHMilestoneState.CLOSED) {
            milestone.close();
            success("Milestone %s closed (%s)", milestone.getTitle(), milestone.getHtmlUrl());
        } else {
            success("Milestone %s already closed (%s)", milestone.getTitle(), milestone.getHtmlUrl());
        }

        // Compute next version
        String nextVersion = getNextVersion(releaseVersion);
        success("Next version will be %s", nextVersion);

        // Create new milestone if it does not already exist
        GHMilestone nextMilestone = milestones.stream().filter(m -> m.getTitle().equals(nextVersion)).findFirst()
                .orElse(null);
        if (nextMilestone != null) {
            success("Next milestone (%s) already exists: %s", nextMilestone.getTitle(), nextMilestone.getHtmlUrl());
        } else {
            nextMilestone = repository.createMilestone(nextVersion, null);
            success("Next milestone (%s) created: %s", nextMilestone.getTitle(), nextMilestone.getHtmlUrl());
        }

        // Compute the release notes and create releases
        List<GHIssue> issues = repository.getIssues(GHIssueState.CLOSED, milestone);
        String description = createReleaseDescription(issues);
        // Check if release already exists
        GHRelease existingRelease = repository.getReleaseByTagName(tag.getName());
        if (existingRelease != null) {
            info("Release %s already exists (%s) - skip release note generation", existingRelease.getName(), existingRelease.getHtmlUrl());
            info("Generated release notes:\n%s", description);

            if (existingRelease.isDraft()) {
                existingRelease.update().draft(false);
                success("Marked release %s as non-draft", existingRelease.getName());
            }
        } else {
            GHRelease release = repository.createRelease(releaseVersion)
                    .name(releaseVersion)
                    .body(description)
                    .create();
            success("Release %s created: %s", releaseVersion, release.getHtmlUrl());
        }

        completed("Post-Release done!");

        return 0;
    }

    private GHTag getTag(String releaseVersion, List<GHTag> tags) {
        failIfTrue(tags::isEmpty, "No tags found in repository");
        Optional<GHTag> first = tags.stream().filter(tag -> tag.getName().equals(releaseVersion)).findFirst();
        if (first.isPresent()) {
            success("Tag %s found", releaseVersion);
            return first.get();
        }
        fail("Unable to find the tag %s in the repository", releaseVersion);
        return null;
    }

    private String getNextVersion(String v) {
        String[] segments = v.split("\\.");
        if (segments.length < 3) {
            fail("Invalid version %s, number of segments must be at least 3, found %d", v, segments.length);
        }

        return String.format("%s.%d.0", segments[0], Integer.parseInt(segments[1]) + 1);
    }

    private String createReleaseDescription(List<GHIssue> issues) throws IOException {
        File file = new File("target/differences.md");
        String compatibility = "";
        if (file.isFile()) {
            compatibility += new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        }

        StringBuilder desc = new StringBuilder();
        desc.append("### Changelog\n\n");
        List<String> list = issues.stream().map(this::line).collect(Collectors.toList());
        desc.append(String.join("\n", list));
        desc.append("\n");

        desc.append(compatibility).append("\n");
        return desc.toString();
    }

    private String line(GHIssue issue) {
        return String.format(" * [#%d](%s) - %s", issue.getNumber(), issue.getHtmlUrl(), issue.getTitle());
    }

}
