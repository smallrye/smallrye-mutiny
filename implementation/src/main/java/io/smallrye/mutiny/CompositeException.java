package io.smallrye.mutiny;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.smallrye.mutiny.groups.UniAndGroup;
import io.smallrye.mutiny.helpers.ParameterValidation;

/**
 * An implementation of {@link Exception} collecting several causes.
 * This class is used to collect multiple failures.
 *
 * Uses {@link #getCauses()} to retrieves the individual causes.
 * {@link #getCause()} returns the first cause.
 *
 * @see UniAndGroup
 */
public class CompositeException extends RuntimeException {

    private final List<Throwable> causes;

    public CompositeException(List<Throwable> causes) {
        super("Multiple exceptions caught:", getFirstOrFail(causes));
        this.causes = Collections.unmodifiableList(causes);
    }

    private static Throwable getFirstOrFail(List<Throwable> causes) {
        if (causes == null || causes.isEmpty()) {
            throw new IllegalArgumentException("Composite Exception must contains at least one cause");
        }
        return ParameterValidation.nonNull(causes.get(0), "cause");
    }

    private static Throwable getFirstOrFail(Throwable[] causes) {
        if (causes == null || causes.length == 0) {
            throw new IllegalArgumentException("Composite Exception must contains at least one cause");
        }
        return ParameterValidation.nonNull(causes[0], "cause");
    }

    public CompositeException(Throwable... causes) {
        super("Multiple exceptions caught:", getFirstOrFail(causes));
        this.causes = Arrays.asList(causes);
    }

    public CompositeException(CompositeException other, Throwable toBeAppended) {
        List<Throwable> c = new ArrayList<>(other.causes);
        c.add(toBeAppended);
        this.causes = Collections.unmodifiableList(c);
        initCause(getFirstOrFail(this.causes));
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
