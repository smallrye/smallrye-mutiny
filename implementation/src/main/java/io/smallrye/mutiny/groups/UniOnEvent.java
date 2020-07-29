package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.smallrye.mutiny.tuples.Functions;

/**
 * Allows configuring the action to execute on each type of events emitted by a {@link Uni} or by
 * a {@link UniSubscriber}
 *
 * @param <T> the type of item emitted by the {@link Uni}
 */
public class UniOnEvent<T> {

    private final Uni<T> upstream;

    public UniOnEvent(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Attaches an action executed when the {@link Uni} has received a {@link UniSubscription} from upstream.
     * The downstream does not have received the subscription yet. It will be done once the action completes.
     * <p>
     * This method is not intended to cancel the subscription. It's the responsibility of the subscriber to do so.
     *
     * @param consumer the callback, must not be {@code null}
     * @return a new {@link Uni}
     * @deprecated Uni {@link Uni#onSubscribe()} instead.
     */
    @Deprecated
    public Uni<T> subscribed(Consumer<? super UniSubscription> consumer) {
        return upstream.onSubscribe().invoke(consumer);
    }

    /**
     * Attaches an action executed when a subscription is cancelled.
     * The upstream is not cancelled yet, but will when the callback completes.
     *
     * @param runnable the callback, must not be {@code null}
     * @return a new {@link Uni}
     * @deprecated Use {@link Uni#onCancellation()} instead.
     */
    @Deprecated
    public Uni<T> cancellation(Runnable runnable) {
        return upstream.onCancellation().invoke(runnable);
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
     * @deprecated Use {@code uni.onTermination().invoke(...)} instead
     */
    @Deprecated
    public Uni<T> termination(Functions.TriConsumer<T, Throwable, Boolean> consumer) {
        return upstream.onTermination().invoke(consumer);
    }

    /**
     * Attaches an action that is executed when the {@link Uni} emits an item or a failure or when the subscriber
     * cancels the subscription. Unlike {@link #termination(Functions.TriConsumer)}, the callback does not receive
     * the item, failure or cancellation.
     *
     * @param action the action to run, must not be {@code null}
     * @return the new {@link Uni}
     * @deprecated Use {@code uni.onTermination().invoke(...)} instead
     */
    @Deprecated
    public Uni<T> termination(Runnable action) {
        return upstream.onTermination().invoke(action);
    }

    /**
     * Configures the action to execute when the observed {@link Uni} emits the item (potentially {@code null}).
     *
     * <p>
     * Examples:
     * </p>
     *
     * <pre>
     * {@code
     * Uni<T> uni = ...;
     * uni.onItem().transform(x -> ...); // Transform the item into another item (~ map)
     * uni.onItem().transformToUni(x -> ...); // Transform the item into a Uni (~ flatMap)
     * }
     * </pre>
     *
     * @return the object to configure the action to execute when an item is emitted
     * @see Uni#ifNoItem()
     */
    public UniOnItem<T> item() {
        return upstream.onItem();
    }

    /**
     * Like {@link #failure(Predicate)} but applied to all failures fired by the upstream uni.
     * It allows configuring the on failure behavior (recovery, retry...).
     *
     * @return a UniOnFailure on which you can specify the on failure action
     */
    public UniOnFailure<T> failure() {
        return upstream.onFailure();
    }

    /**
     * Configures a predicate filtering the failures on which the behavior (specified with the returned
     * {@link UniOnFailure}) is applied.
     * <p>
     * For instance, to only when an {@code IOException} is fired as failure you can use:
     * <code>uni.onFailure(IOException.class).recoverWithItem("hello")</code>
     * <p>
     * The fallback value ({@code hello}) will only be used if the upstream uni fire a failure of type
     * {@code IOException}.*
     *
     * @param predicate the predicate, {@code null} means applied to all failures
     * @return a UniOnFailure configured with the given predicate on which you can specify the on failure action
     */
    public UniOnFailure<T> failure(Predicate<? super Throwable> predicate) {
        return upstream.onFailure(predicate);
    }

    /**
     * Configures a type of failure filtering the failures on which the behavior (specified with the returned
     * {@link UniOnFailure}) is applied.
     * <p>
     * For instance, to only when an {@code IOException} is fired as failure you can use:
     * <code>uni.onFailure(IOException.class).recoverWithItem("hello")</code>
     * <p>
     * The fallback value ({@code hello}) will only be used if the upstream uni fire a failure of type
     * {@code IOException}.*
     *
     * @param typeOfFailure the class of exception, must not be {@code null}
     * @return a UniOnFailure configured with the given predicate on which you can specify the on failure action
     */
    public UniOnFailure<T> failure(Class<? extends Throwable> typeOfFailure) {
        return upstream.onFailure(typeOfFailure);
    }

    /**
     * Configures actions when the subscription is cancelled.
     *
     * @return the object to configure the actions
     */
    public UniOnCancel<T> cancellation() {
        return upstream.onCancellation();
    }

    /**
     * Configure actions when receiving a subscription.
     *
     * @return the object to configure the actions
     */
    public UniOnSubscribe<T> subscribe() {
        return upstream.onSubscribe();
    }

    /**
     * Configures actions when the {@link Uni} terminates on either an item, a failure or a cancellation.
     *
     * @return the object to configure the actions
     */
    public UniOnTerminate<T> termination() {
        return upstream.onTermination();
    }

    /**
     * Configures actions when receiving either an item or a failure.
     *
     * @return the object to configure the actions
     */
    public UniOnItemOrFailure<T> itemOrFailure() {
        return upstream.onItemOrFailure();
    }
}
