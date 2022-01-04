package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.UniOnTermination;
import io.smallrye.mutiny.operators.uni.UniOnTerminationCall;
import io.smallrye.mutiny.tuples.Functions;

public class UniOnTerminate<T> {

    private final Uni<T> upstream;

    public UniOnTerminate(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Attaches an action that is executed when the {@link Uni} emits an item or a failure or when the subscriber
     * cancels the subscription.
     *
     * @param consumer the consumer receiving the item, the failure and a boolean indicating whether the termination
     *        is due to a cancellation (the 2 first parameters would be {@code null} in this case). Must not
     *        be {@code null} If the second parameter (the failure) is not {@code null}, the first is
     *        necessary {@code null} and the third is necessary {@code false} as it indicates a termination
     *        due to a failure.
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> invoke(Functions.TriConsumer<T, Throwable, Boolean> consumer) {
        Functions.TriConsumer<T, Throwable, Boolean> actual = Infrastructure.decorate(nonNull(consumer, "consumer"));
        return Infrastructure.onUniCreation(new UniOnTermination<>(upstream, actual));
    }

    /**
     * Attaches an action that is executed when the {@link Uni} emits an item or a failure or when the subscriber
     * cancels the subscription. Unlike {@link #invoke(Functions.TriConsumer)} (Functions.TriConsumer)}, the callback does not
     * receive
     * the item, failure or cancellation.
     *
     * @param action the action to run, must not be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> invoke(Runnable action) {
        Runnable runnable = Infrastructure.decorate(nonNull(action, "action"));
        return Infrastructure.onUniCreation(new UniOnTermination<>(upstream, (i, f, c) -> runnable.run()));
    }

    /**
     * Attaches an action that is executed when the {@link Uni} emits an item or a failure or when the subscriber
     * cancels the subscription.
     *
     * @param mapper the function receiving the item, the failure and a boolean indicating whether the termination
     *        is due to a cancellation. When an item is emitted then the failure is {@code null} and the boolean
     *        is {@code false}. When a failure is emitted then the item is {@code null} and the boolean
     *        is {@code false}. When the subscription has been cancelled then the boolean is {@code true} and the
     *        other parameters are {@code null}.
     *        The function must return a non-{@code null} {@link Uni}.
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> call(Functions.Function3<? super T, Throwable, Boolean, Uni<?>> mapper) {
        Functions.Function3<? super T, Throwable, Boolean, Uni<?>> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        return Infrastructure
                .onUniCreation(new UniOnTerminationCall<>(upstream, actual));
    }

    /**
     * Attaches an action that is executed when the {@link Uni} emits an item or a failure or when the subscriber
     * cancels the subscription. Unlike {@link #call(Functions.Function3)} the supplier does not receive the
     * item, failure or cancellation.
     *
     * @param supplier must return a non-{@code null} {@link Uni}.
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> call(Supplier<Uni<?>> supplier) {
        Supplier<Uni<?>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return call((i, f, c) -> actual.get());
    }

}
