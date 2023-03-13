package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.BooleanSupplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.uni.UniMemoizeOp;

public class UniMemoize<T> {

    private final Uni<T> upstream;

    public UniMemoize(AbstractUni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Memoize the received item or failure as long as the provided boolean supplier evaluates to {@code false}.
     * <p>
     * New subscribers will receive the memoized item or failure.
     * When the boolean supplier evaluates to {@code true} then a new upstream subscription happens and the next
     * subscribers get a chance to observe new values.
     *
     * @param invalidationGuard the invalidation guard, which evaluates to {@code false} for as long as the item or failure must
     *        be memoized, must not be {@code null}
     * @return a new {@link Uni}
     * @apiNote This is an experimental API
     */
    @CheckReturnValue
    public Uni<T> until(BooleanSupplier invalidationGuard) {
        BooleanSupplier actual = Infrastructure.decorate(nonNull(invalidationGuard, "invalidationGuard"));
        return Infrastructure.onUniCreation(new UniMemoizeOp<>(upstream, actual));
    }

    /**
     * Memoize the received item or failure for a duration after the upstream subscription has been received.
     * <p>
     * New subscribers will receive the memoized item or failure.
     * When duration has elapsed then the first subscription causes a new upstream subscription, and the next
     * subscribers get a chance to observe new values.
     *
     * @param duration the memoization duration after having received the subscription from upstream, must not be
     *        {@code null}, must be strictly positive
     * @return a new {@link Uni}
     * @apiNote This is an experimental API
     */
    @CheckReturnValue
    public Uni<T> atLeast(Duration duration) {
        Duration validatedDuration = validate(duration, "duration");
        return until(new BooleanSupplier() {
            private volatile long startTime = -1;

            @Override
            public boolean getAsBoolean() {
                long now = System.nanoTime();
                if (startTime == -1) {
                    startTime = now;
                }

                // Avoid arithmetic overflow when retrieving the nanos of the FOREVER duration
                if (validatedDuration == ChronoUnit.FOREVER.getDuration()) {
                    startTime = now;
                    return false;
                }

                boolean invalidates = (now - startTime) > validatedDuration.toNanos();
                if (invalidates) {
                    startTime = now;
                }
                return invalidates;
            }
        });
    }

    /**
     * Memoize the received item or failure indefinitely.
     *
     * @return a new {@link Uni}
     * @apiNote This is an experimental API
     */
    @CheckReturnValue
    public Uni<T> indefinitely() {
        return this.until(() -> false);
    }
}
