package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;

public class UniIfNoItem<T> {

    private final Uni<T> upstream;

    public UniIfNoItem(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Configures the timeout duration.
     *
     * @param timeout the timeout, must not be {@code null}, must be strictly positive.
     * @return a new {@link UniIfNoItem}
     */
    @CheckReturnValue
    public UniOnTimeout<T> after(Duration timeout) {
        return new UniOnTimeout<>(upstream, validate(timeout, "timeout"), null);
    }

}
