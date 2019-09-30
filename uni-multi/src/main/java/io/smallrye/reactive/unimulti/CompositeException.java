package io.smallrye.reactive.unimulti;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.smallrye.reactive.unimulti.groups.UniAndGroup;

/**
 * An implementation of {@link Exception} collecting several causes.
 * Uses {@link #getCauses()} to retrieves the individual causes.
 *
 * @see UniAndGroup
 */
public class CompositeException extends RuntimeException {

    private final List<Throwable> causes;

    public CompositeException(List<Throwable> causes) {
        super("Multiple exceptions caught:");
        this.causes = Collections.unmodifiableList(causes);
    }

    public CompositeException(Throwable... causes) {
        super("Multiple exceptions caught:");
        this.causes = Arrays.asList(causes);
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        for (int i = 0; i < causes.size(); i++) {
            Throwable cause = causes.get(i);
            message = message + "\n\t[Exception " + i + "] " + cause;
        }
        return message;
    }

    public List<Throwable> getCauses() {
        return causes;
    }
}
