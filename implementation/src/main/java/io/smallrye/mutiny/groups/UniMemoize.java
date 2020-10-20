package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.function.BooleanSupplier;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniMemoizeOp;

@Experimental("Memoization is an experimental feature at this stage")
public class UniMemoize<T> {

    private final Uni<T> upstream;

    public UniMemoize(AbstractUni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Memoize the received item or failure as long as the provided boolean supplier evaluates to {@code false}.
     * <p>
     * New subscribers will receive the memoize item or failure.
     * When the boolean supplier evaluates to {@code true} then a new upstream subscription happens and the subscribers get a
     * chance to observe new values.
     *
     * @param invalidationGuard the invalidation guard, which evaluates to {@code false} for as long as the item or failure must
     *        be memoized
     * @return a new {@link Uni}
     */
    public Uni<T> until(BooleanSupplier invalidationGuard) {
        return Infrastructure.onUniCreation(new UniMemoizeOp<>(upstream, invalidationGuard));
    }

    /**
     * Memoize the received item or failure for a duration after the upstream subscription has been received.
     * <p>
     * New subscribers will receive the memoize item or failure.
     * When duration has elapsed then a new upstream subscription happens and the subscribers get a chance to observe new
     * values.
     *
     * @param duration the memoization duration after having received the subscription from upstream
     * @return a new {@link Uni}
     */
    public Uni<T> atLeast(Duration duration) {
        return until(new BooleanSupplier() {
            private volatile long startTime = -1;

            @Override
            public boolean getAsBoolean() {
                if (startTime == -1) {
                    startTime = System.currentTimeMillis();
                }
                boolean invalidates = (System.currentTimeMillis() - startTime) > duration.toMillis();
                if (invalidates) {
                    startTime = System.currentTimeMillis();
                }
                return invalidates;
            }
        });
    }

    /**
     * Memoize the received item or failure indefinitely.
     * 
     * @return a new {@link Uni}
     */
    public Uni<T> indefinitely() {
        return this.until(() -> false);
    }
}
