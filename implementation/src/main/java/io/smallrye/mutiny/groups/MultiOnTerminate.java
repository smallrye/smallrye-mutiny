package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOnTerminationCall;
import io.smallrye.mutiny.operators.multi.MultiOnTerminationInvoke;

public class MultiOnTerminate<T> {

    private final Multi<T> upstream;

    public MultiOnTerminate(Multi<T> upstream) {
        this.upstream = ParameterValidation.nonNull(upstream, "upstream");
    }

    /**
     * Attaches an action that is executed when the {@link Multi} emits a completion or a failure or when the subscriber
     * cancels the subscription.
     *
     * @param callback the consumer receiving the failure if any and a boolean indicating whether the termination
     *        is due to a cancellation (the failure parameter would be {@code null} in this case). Must not
     *        be {@code null}.
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> invoke(BiConsumer<Throwable, Boolean> callback) {
        BiConsumer<Throwable, Boolean> actual = Infrastructure.decorate(nonNull(callback, "callback"));
        return Infrastructure.onMultiCreation(new MultiOnTerminationInvoke<>(upstream, actual));
    }

    /**
     * Attaches an action that is executed when the {@link Multi} emits a completion or a failure or when the subscriber
     * cancels the subscription. Unlike {@link #invoke(BiConsumer)}, the callback does not receive the failure or
     * cancellation details.
     *
     * @param action the action to execute when the streams completes, fails or the subscription gets cancelled. Must
     *        not be {@code null}.
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> invoke(Runnable action) {
        Runnable runnable = Infrastructure.decorate(nonNull(action, "action"));
        return Infrastructure.onMultiCreation(new MultiOnTerminationInvoke<>(upstream, (f, c) -> runnable.run()));
    }

    /**
     * Attaches an action that is executed when the {@link Multi} emits a completion or a failure or when the subscriber
     * cancels the subscription.
     *
     * @param mapper the function to execute where the first argument is a non-{@code null} exception on failure, and
     *        the second argument is a boolean which is {@code true} when the subscriber cancels the subscription.
     *        The function returns a {@link Uni} and must not be {@code null}.
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> call(BiFunction<Throwable, Boolean, Uni<?>> mapper) {
        BiFunction<Throwable, Boolean, Uni<?>> actual = Infrastructure
                .decorate(Infrastructure.decorate(nonNull(mapper, "mapper")));
        return Infrastructure.onMultiCreation(new MultiOnTerminationCall<>(upstream, actual));
    }

    /**
     * Attaches an action that is executed when the {@link Multi} emits a completion or a failure or when the subscriber
     * cancels the subscription.
     *
     * @param supplier the supplier returns a {@link Uni} and must not be {@code null}.
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> call(Supplier<Uni<?>> supplier) {
        Supplier<Uni<?>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return call((ignoredFailure, ignoredCancellation) -> actual.get());
    }

}
