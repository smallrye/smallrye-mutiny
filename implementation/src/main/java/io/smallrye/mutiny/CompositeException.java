package io.smallrye.mutiny;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.smallrye.mutiny.groups.UniAndGroup;
import io.smallrye.mutiny.helpers.ParameterValidation;

/**
 * An implementation of {@link Exception} collecting several causes.
 * This class is used to collect multiple failures.
 * <p>
 * Uses {@link #getCauses()} to retrieves the individual causes.
 * {@link #getCause()} returns the first cause.
 * <p>
 * Causes, except the first one, are stored as suppressed exception.
 *
 * @see UniAndGroup
 */
public class CompositeException extends RuntimeException {

    public CompositeException(List<Throwable> causes) {
        super("Multiple exceptions caught:", getFirstOrFail(causes));
        for (int i = 1; i < causes.size(); i++) {
            addSuppressed(causes.get(i));
        }
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
        for (int i = 1; i < causes.length; i++) {
            addSuppressed(causes[i]);
        }
    }

    public CompositeException(CompositeException other, Throwable toBeAppended) {
        Throwable[] suppressed = other.getSuppressed();
        for (Throwable throwable : suppressed) {
            addSuppressed(throwable);
        }
        addSuppressed(toBeAppended);
        initCause(other.getCause());
    }

    @Override
    public String getMessage() {
        String messageFromSuper = super.getMessage();
        StringBuilder message;
        if (messageFromSuper != null) {
            message = new StringBuilder(messageFromSuper);
        } else {
            message = new StringBuilder();
        }
        message.append("\n\t[Exception 0] ").append(getCause());
        Throwable[] suppressed = getSuppressed();
        for (int i = 0; i < suppressed.length; i++) {
            Throwable cause = suppressed[i];
            message.append("\n\t[Exception ").append(i + 1).append("] ").append(cause);
        }
        return message.toString();
    }

    public List<Throwable> getCauses() {
        List<Throwable> causes = new ArrayList<>();
        causes.add(getCause());
        causes.addAll(Arrays.asList(getSuppressed()));
        return causes;
    }
}
