///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.kohsuke:github-api:1.326
//DEPS info.picocli:picocli:4.7.6

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "UpdateDocsAttributesFiles", mixinStandardHelpOptions = true, description = "Update the documentation attributes file")
public class UpdateDocsAttributesFiles implements Callable<Integer> {

    @Option(names = "--mutiny-version", description = "Pass a Mutiny version instead of fetching it from GitHub")
    String mutinyVersion;

    @Option(names = "--vertx-bindings-version", description = "Pass a Vert.x bindings version instead of fetching it from GitHub")
    String vertxBindingsVersion;

    @Override
    public Integer call() throws Exception {
        var gh = new GitHubBuilder().build();
        var lastMutinyRelease = (mutinyVersion == null) ? getRelease(gh, "smallrye/smallrye-mutiny") : mutinyVersion;
        var lastBindingsRelease = (vertxBindingsVersion == null) ? getRelease(gh, "smallrye/smallrye-mutiny-vertx-bindings") : vertxBindingsVersion;

        var newline = "\n";
        var builder = new StringBuilder();
        builder
                .append("// GENERATED FILE, DO NOT EDIT DIRECTLY (see .build/UpdateDocsAttributesFiles.java)").append(newline)
                .append(":project-version: ").append(lastMutinyRelease).append(newline)
                .append(":versions-mutiny: ").append(lastMutinyRelease).append(newline)
                .append(":versions-vertx-bindings: ").append(lastBindingsRelease).append(newline);

        Files.writeString(Path.of("documentation/src/main/asciidoc/_versions.adoc"), builder.toString(), StandardCharsets.UTF_8);
        return 0;
    }

    private String getRelease(GitHub gh, String name) throws IOException {
        return gh.getRepository(name).getLatestRelease().getName();
    }

    public static void main(String... args) {
        var status = new CommandLine(new UpdateDocsAttributesFiles()).execute(args);
        System.exit(status);
    }
}
