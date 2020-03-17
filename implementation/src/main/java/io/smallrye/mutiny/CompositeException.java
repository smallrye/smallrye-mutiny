package io.smallrye.mutiny;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.smallrye.mutiny.groups.UniAndGroup;

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

    public CompositeException(CompositeException other, Throwable toBeAppended) {
        List<Throwable> c = new ArrayList<>(other.causes);
        c.add(toBeAppended);
        this.causes = Collections.unmodifiableList(c);
    }

    @Override
    public String getMessage() {
        StringBuilder message = Optional.ofNullable(super.getMessage()).map(StringBuilder::new).orElse(null);
        for (int i = 0; i < causes.size(); i++) {
            Throwable cause = causes.get(i);
            message = (message == null ? new StringBuilder("null") : message).append("\n\t[Exception ").append(i)
                    .append("] ").append(cause);
        }
        return message == null ? null : message.toString();
    }

    public List<Throwable> getCauses() {
        return causes;
    }
}
