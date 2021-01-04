package helpers;

import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTag;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Supplier;


public class Helper {

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

    public static void failIfTrue(Supplier<Boolean> predicate, String message, Object... params) {
        if (predicate.get()) {
            fail(message, params);
        }
    }

    public static <T> T first(Collection<T> collection) {
        if (collection.isEmpty()) {
            fail("Cannot retrieve the first item from an empty collection");
        }
        return collection.iterator().next();
    }

    public static String computeNextVersion(GHRepository repository, GHTag tag, boolean micro) throws IOException {
        info("Retrieving release associated with tag %s", tag.getName());
        GHRelease name = repository.getReleaseByTagName(tag.getName());
        if (name == null) {
            warn("No release associated with tag %s", tag.getName());
        }
        String[] segments = tag.getName().split("\\.");
        if (segments.length < 3) {
            fail("Invalid version %s, number of segments must be at least 3, found %d", tag.getName(), segments.length);
        }

        if (micro) {
            return String.format("%s.%s.%d", segments[0], segments[1], Integer.parseInt(segments[2]) + 1);
        } else {
            return String.format("%s.%d.0", segments[0], Integer.parseInt(segments[1]) + 1);
        }
    }
}