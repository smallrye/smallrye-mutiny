package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;

public class MultiIfNoItem<T> {

    private final Multi<T> upstream;

    public MultiIfNoItem(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Configures the timeout duration.
     *
     * @param timeout the timeout, must not be {@code null}, must be strictly positive.
     * @return a new {@link MultiOnItemTimeout}
     */
    @CheckReturnValue
    public MultiOnItemTimeout<T> after(Duration timeout) {
        return new MultiOnItemTimeout<>(upstream, validate(timeout, "timeout"), null);
    }
}
