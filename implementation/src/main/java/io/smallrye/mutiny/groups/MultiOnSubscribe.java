package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOnSubscribeCall;
import io.smallrye.mutiny.operators.multi.MultiOnSubscribeInvokeOp;

/**
 * Group to configure the action to execute when the observed {@link Multi} sends a {@link Subscription}.
 * The downstream don't have a subscription yet. It will be passed once the configured action completes.
 * <p>
 * For example:
 *
 * <pre>
 * {@code
 * multi.onSubscription().invoke(sub -> System.out.println("subscribed"));
 * // Delay the subscription by 1 second (or until an asynchronous action completes)
 * multi.onSubscription().call(sub -> Uni.createFrom(1).onItem().delayIt().by(Duration.ofSecond(1)));
 * }
 * </pre>
 *
 * @param <T> the type of item
 */
public class MultiOnSubscribe<T> {

    private final Multi<T> upstream;

    public MultiOnSubscribe(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when the {@code subscription} is received.
     * <p>
     * The callback in invoked before passing a subscription event downstream.
     * If the callback throws an exception, the downstream receives a subscription and the failure immediately.
     *
     * @param callback the callback, must not be {@code null}.
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> invoke(Consumer<? super Subscription> callback) {
        Consumer<? super Subscription> actual = Infrastructure.decorate(nonNull(callback, "callback"));
        return Infrastructure.onMultiCreation(
                new MultiOnSubscribeInvokeOp<>(upstream, actual));
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when the {@code subscription} is received.
     * <p>
     * The callback in invoked before passing a subscription event downstream.
     * If the callback throws an exception, the downstream receives a subscription and the failure immediately.
     *
     * @param callback the callback, must not be {@code null}.
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> invoke(Runnable callback) {
        Runnable actual = nonNull(callback, "callback");
        // Decoration happens in `invoke`
        return invoke(ignored -> actual.run());
    }

    /**
     * Produces a new {@link Multi} invoking the given @{code action} when the {@code subscription} event is received.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * the subscription, the function is called. The subscription event is passed downstream only when the {@link Uni}
     * completes. If the produced {@code Uni} fails or if the function throws an exception, the failure is propagated
     * downstream.
     *
     * @param action the callback, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> call(Function<? super Subscription, Uni<?>> action) {
        Function<? super Subscription, Uni<?>> actual = Infrastructure.decorate(nonNull(action, "action"));
        return Infrastructure.onMultiCreation(
                new MultiOnSubscribeCall<>(upstream, actual));
    }

    /**
     * Produces a new {@link Multi} invoking the given @{code action} when the {@code subscription} event is received.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * the subscription, the supplier is called. The subscription event is passed downstream only when the {@link Uni}
     * completes. If the produced {@code Uni} fails or if the function throws an exception, the failure is propagated
     * downstream.
     *
     * @param action the callback, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> call(Supplier<Uni<?>> action) {
        Supplier<Uni<?>> actual = Infrastructure.decorate(nonNull(action, "action"));
        return call(ignored -> actual.get());
    }
}
