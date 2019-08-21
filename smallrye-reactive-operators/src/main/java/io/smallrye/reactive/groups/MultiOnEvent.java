package io.smallrye.reactive.groups;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.*;
import org.reactivestreams.Subscription;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

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
     * @param consumer the callback, must not be {@code null}
     * @return a new {@link Multi}
     */
    public Multi<T> subscription(Consumer<? super Subscription> consumer) {
        return new MultiOnSubscription<>(upstream, nonNull(consumer, "consumer"));
    }

    /**
     * Attaches an action executed when a subscription is cancelled.
     * The upstream is not cancelled yet, but will when the callback completes.
     *
     * @param runnable the callback, must not be {@code null}
     * @return a new {@link Multi}
     */
    public Multi<T> cancellation(Runnable runnable) {
        return new MultiOnCancellation<>(upstream, nonNull(runnable, "runnable"));
    }

    public Multi<T> request(LongConsumer consumer) {
        return new MultiOnRequest<>(upstream, consumer);
    }

    public MultiOverflow overflow() {
        return new MultiOverflow<>(upstream);
    }

    /**
     * Attaches an action that is executed when the {@link Multi} emits a completion or a failure or when the subscriber
     * cancels the subscription.
     *
     * @param consumer the consumer receiving the failure if any and a boolean indicating whether the termination
     *                 is due to a cancellation (the failure parameter would be {@code null} in this case). Must not
     *                 be {@code null}.
     * @return the new {@link Multi}
     */
    public Multi<T> termination(BiConsumer<Throwable, Boolean> consumer) {
        return new MultiOnTermination<>(upstream, nonNull(consumer, "consumer"));
    }

    /**
     * Configures the action to execute when the observed {@link Multi} emits the item (potentially {@code null}).
     *
     * <p>Examples:</p>
     * <pre>{@code
     * Multi<T> multi = ...;
     * multi.onItem().mapToItem(x -> ...); // Map to another item
     * multi.onItem().mapToUni(x -> ...); // Map to a multi
     * }</pre>
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
     * Configures a predicate filtering the failures on which the behavior (specified with the returned
     * {@link MultiOnFailure}) is applied.
     * <p>
     * For instance, to only when an {@code IOException} is fired as failure you can use:
     * <code>multi.onFailure(IOException.class).recoverWithResult("hello")</code>
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
     * <code>multi.onFailure(IOException.class).recoverWithResult("hello")</code>
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

    public Multi<T> completion(Runnable action) {
        return new MultiOnCompletionPeek<>(upstream, action);
    }
}
