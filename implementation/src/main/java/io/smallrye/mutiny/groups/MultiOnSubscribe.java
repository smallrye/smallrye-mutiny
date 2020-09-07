package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOnSubscribeInvokeOp;
import io.smallrye.mutiny.operators.multi.MultiOnSubscribeInvokeUniOp;

/**
 * Group to configure the action to execute when the observed {@link Multi} sends a {@link Subscription}.
 * The downstream don't have a subscription yet. It will be passed once the configured action completes.
 *
 * For example:
 *
 * <pre>
 * {@code
 * multi.onSubscribe().invoke(sub -> System.out.println("subscribed"));
 * // Delay the subscription by 1 second (or until an asynchronous action completes)
 * multi.onSubscribe().call(sub -> Uni.createFrom(1).onItem().delayIt().by(Duration.ofSecond(1)));
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
    public Multi<T> invoke(Consumer<? super Subscription> callback) {
        return Infrastructure.onMultiCreation(
                new MultiOnSubscribeInvokeOp<>(upstream, nonNull(callback, "callback")));
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
    public Multi<T> invoke(Runnable callback) {
        Runnable actual = nonNull(callback, "callback");
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
    public Multi<T> call(Function<? super Subscription, Uni<?>> action) {
        return Infrastructure.onMultiCreation(
                new MultiOnSubscribeInvokeUniOp<>(upstream, nonNull(action, "action")));
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
    public Multi<T> call(Supplier<Uni<?>> action) {
        Supplier<Uni<?>> actual = nonNull(action, "action");
        return call(ignored -> actual.get());
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
     * @deprecated Use {@link #call(Function)}
     */
    @Deprecated
    public Multi<T> invokeUni(Function<? super Subscription, Uni<?>> action) {
        return call(action);
    }
}
