package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOnSubscribeInvokeOp;
import io.smallrye.mutiny.operators.multi.MultiSignalConsumerOp;

/**
 * Allows configuring the action to execute on each type of events emitted by a {@link Multi} or by
 * a {@link org.reactivestreams.Subscriber}
 *
 * @param <T> the type of item emitted by the {@link Multi}
 */
public class MultiOnEvent<T> {

    private final Multi<T> upstream;

    public MultiOnEvent(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Attaches an action executed when the {@link Multi} has received a {@link Subscription} from upstream.
     * The downstream does not have received the subscription yet. It will be done once the action completes.
     * <p>
     * This method is not intended to cancel the subscription. It's the responsibility of the subscriber to do so.
     *
     * @param callback the callback, must not be {@code null}
     * @return a new {@link Multi}
     * @deprecated Use {@link Multi#onSubscribe()}
     */
    @Deprecated
    public Multi<T> subscribed(Consumer<? super Subscription> callback) {
        return Infrastructure.onMultiCreation(new MultiOnSubscribeInvokeOp<>(upstream, callback));
    }

    /**
     * Attaches an action executed when a subscription is cancelled.
     * The upstream is not cancelled yet, but will when the callback completes.
     *
     * @param callback the callback, must not be {@code null}
     * @return a new {@link Multi}
     * @deprecated Use {@link Multi#onCancellation()} instead.
     */
    @Deprecated
    public Multi<T> cancellation(Runnable callback) {
        return upstream.onCancellation().invoke(callback);
    }

    public Multi<T> request(LongConsumer callback) {
        return Infrastructure.onMultiCreation(new MultiSignalConsumerOp<>(
                upstream,
                null,
                null,
                null,
                nonNull(callback, "callback"),
                null));
    }

    public MultiOverflow<T> overflow() {
        return new MultiOverflow<>(upstream);
    }

    /**
     * Attaches an action that is executed when the {@link Multi} emits a completion or a failure or when the subscriber
     * cancels the subscription.
     *
     * @param callback the consumer receiving the failure if any and a boolean indicating whether the termination
     *        is due to a cancellation (the failure parameter would be {@code null} in this case). Must not
     *        be {@code null}.
     * @return the new {@link Multi}
     * @deprecated use {{@link Multi#onTermination()}}
     */
    @Deprecated
    public Multi<T> termination(BiConsumer<Throwable, Boolean> callback) {
        return upstream.onTermination().invoke(callback);
    }

    /**
     * Attaches an action that is executed when the {@link Multi} emits a completion or a failure or when the subscriber
     * cancels the subscription. Unlike {@link #termination(BiConsumer)}, the callback does not receive the failure or
     * cancellation details.
     *
     * @param action the action to execute when the streams completes, fails or the subscription gets cancelled. Must
     *        not be {@code null}.
     * @return the new {@link Multi}
     * @deprecated use {{@link Multi#onTermination()}}
     */
    @Deprecated
    public Multi<T> termination(Runnable action) {
        return upstream.onTermination().invoke(action);
    }

    /**
     * Configures the action to execute when the observed {@link Multi} emits an item.
     *
     * <p>
     * Examples:
     * </p>
     * 
     * <pre>
     * {@code
     * Multi<T> multi = ...;
     * multi.onItem().apply(x -> ...); // Map to another item
     * multi.onItem().mapToMulti(x -> ...); // Map to a multi
     * }
     * </pre>
     *
     * @return the object to configure the action to execute when an item is emitted
     */
    public MultiOnItem<T> item() {
        return upstream.onItem();
    }

    /**
     * Like {@link #failure(Predicate)} but applied to all failures fired by the upstream multi.
     * It allows configuring the on failure behavior (recovery, retry...).
     *
     * @return a MultiOnFailure on which you can specify the on failure action
     */
    public MultiOnFailure<T> failure() {
        return upstream.onFailure();
    }

    /**
     * Configures the action to execute when the observed {@link Multi} emits the completion event.
     *
     * @return the object to configure the action
     */
    public MultiOnCompletion<T> completion() {
        return upstream.onCompletion();
    }

    /**
     * Configures a predicate filtering the failures on which the behavior (specified with the returned
     * {@link MultiOnFailure}) is applied.
     * <p>
     * For instance, to only when an {@code IOException} is fired as failure you can use:
     * <code>multi.onFailure(IOException.class).recoverWithItem("hello")</code>
     * <p>
     * The fallback value ({@code hello}) will only be used if the upstream multi fires a failure of type
     * {@code IOException}.
     *
     * @param predicate the predicate, {@code null} means applied to all failures
     * @return a MultiOnFailure configured with the given predicate on which you can specify the on failure action
     */
    public MultiOnFailure<T> failure(Predicate<? super Throwable> predicate) {
        return upstream.onFailure(predicate);
    }

    /**
     * Configures a type of failure filtering the failures on which the behavior (specified with the returned
     * {@link MultiOnFailure}) is applied.
     * <p>
     * For instance, to only when an {@code IOException} is fired as failure you can use:
     * <code>multi.onFailure(IOException.class).recoverWithItem("hello")</code>
     * <p>
     * The fallback value ({@code hello}) will only be used if the upstream multi fire a failure of type
     * {@code IOException}.*
     *
     * @param typeOfFailure the class of exception, must not be {@code null}
     * @return a MultiOnFailure configured with the given predicate on which you can specify the on failure action
     */
    public MultiOnFailure<T> failure(Class<? extends Throwable> typeOfFailure) {
        return upstream.onFailure(typeOfFailure);
    }

    /**
     * Configures a callback when this {@link Multi} completes.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Multi}
     * @deprecated Use {@link Multi#onCompletion()} instead
     */
    @Deprecated
    public Multi<T> completion(Runnable callback) {
        return upstream.onCompletion().invoke(callback);
    }

    /**
     * Configure actions when receiving a subscription.
     * 
     * @return the object to configure the actions
     */
    public MultiOnSubscribe<T> subscribe() {
        return upstream.onSubscribe();
    }

    /**
     * Configures actions when the subscription is cancelled.
     *
     * @return the object to configure the actions
     */
    public MultiOnCancel<T> cancellation() {
        return upstream.onCancellation();
    }

    /**
     * Configures actions when the {@link Multi} terminates on either a completion, a failure or a cancellation.
     * 
     * @return the object to configure the actions
     */
    public MultiOnTerminate<T> termination() {
        return upstream.onTermination();
    }
}
