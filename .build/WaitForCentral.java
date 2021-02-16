///usr/bin/env jbang "$0" "$@" ; exit $? # (1)
//DEPS io.vertx:vertx-core:3.9.4
//DEPS info.picocli:picocli:4.5.0
//DEPS io.smallrye.reactive:smallrye-mutiny-vertx-web-client:1.4.0


import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;


@Command(name = "wait-for-central", mixinStandardHelpOptions = true, version = "0.1",
        description = "Wait until artifacts lands on Maven Central:\n./WaitForCentral.java --artifacts=io.smallrye.reactive:mutiny,io.smallrye.reactive:mutiny-context-propagation --expected-version=0.13.0")
class WaitForCentral implements Callable<Integer> {

    @Option(names = "--expected-version" , required = true, description = "The version of the artifact to wait")
    private String version;

    @Option(names = "--artifacts", required = true, split = ",", description = "The comma-separated list of artifact to watch using the `groupId:artifactId` syntax")
    private List<String> artifacts;

    @Option(names= "--timeout", defaultValue = "3600", description = "Amount of seconds before giving up")
    private int timeout;

    @Option(names = "--repository", defaultValue = "https://repo1.maven.org/maven2", description = "The url of the repository")
    private String repository;


    public static void main(String... args) {
        int exitCode = new CommandLine(new WaitForCentral()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        info("Waiting at most %d seconds for artifacts %s version %s to land in repository (%s)", timeout, artifacts, version, repository);
        List<String> urls = new ArrayList<>();
        for (String artifact : artifacts) {
            String[] segments = artifact.split(":");
            if (segments.length != 2) {
                fail("Invalid artifact (%s), must be groupId:artifactId", artifact);
            }
            String groupId = segments[0].replace(".", "/");
            String artifactId = segments[1];
            String url = String.format("%s/%s/%s/%s/%s-%s.pom", repository, groupId, artifactId, version, artifactId, version);
            urls.add(url);
            info("Url for %s: %s", artifact, url);
        }


        Vertx vertx = Vertx.vertx();
        WebClient client = WebClient.create(vertx);
        long beginning = System.currentTimeMillis();
        long max = beginning + (timeout * 1000);


        boolean done = false;
        while (! done && System.currentTimeMillis() < max) {
            try {
                if (get(client, urls)) {
                    done = true;
                } else {
                    info("Next attempt in 30s");
                    Thread.sleep(30000);
                }
            } catch (Exception e) {
                warn("Failed to retrieve artifacts: %s", e);
            }
        }
    
        if (! done) {
            fail("Artifacts still not available after %d seconds", timeout);
        } else {
            info("Artifacts found on the repository!");
        }
        client.close();
        vertx.closeAndAwait();
        if (! done) {
            return -1;
        }
        return 0;
    }

    static class Result {
        final String artifact;
        final int status;

        Result(String artifact, int status) {
            this.artifact = artifact;
            this.status = status;
        }
    }

    boolean get(WebClient client, List<String> urls) {
        List<Uni<Result>> unis = new ArrayList<>();
        for (String url : urls) {
            unis.add(client.getAbs(url).send()
                .onItem().transform(resp -> new Result(url, resp.statusCode()))
            );        
        }

        Uni<Boolean> completed = Uni.combine().all().unis(unis)
            .combinedWith(list -> {
                boolean done = true;
                for (Object r : list) {
                    Result res = (Result) r;
                    if (res.status != 200) {
                        info("Missing artifact: " + res.artifact);
                        done = done && false;
                    }
                }
                return done;
            });

        return completed.await().atMost(Duration.ofSeconds(10));
    }

    public static void info(String msg, Object... params) {
        System.out.println("[INFO] " + String.format(msg, params));
    }

    public static void completed(String msg, Object... params) {
        System.out.println("[DONE] " + String.format(msg, params));
    }

    public static void success(String msg, Object... params) {
        System.out.println("[WARN] " + String.format(msg, params));
    }

    public static void warn(String msg, Object... params) {
        System.out.println("[WARN] " + String.format(msg, params));
    }

    public static void fail(String msg, Object... params) {
        fail(String.format(msg, params), 2);
    }

    public static void fail(String msg, int code) {
        System.out.println("[FAIL] " + msg);
        System.exit(code);
    }


}