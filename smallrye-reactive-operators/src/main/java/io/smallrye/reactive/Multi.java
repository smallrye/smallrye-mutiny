package io.smallrye.reactive;

import io.smallrye.reactive.groups.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Multi<T> extends Publisher<T> {

    static MultiCreate createFrom() {
        return MultiCreate.INSTANCE;
    }

    /**
     * Creates new instances of {@link Multi} by merging, concatenating or associating items from others {@link Multi}
     * and {@link Publisher}.
     *
     * @return the object to configure the creation process.
     */
    static MultiCreateBy createBy() {
        return MultiCreateBy.INSTANCE;
    }

    MultiSubscribe<T> subscribe();

    MultiOnItem<T> onItem();

    /**
     * Creates a {@link Uni} from this {@link Multi}.
     * <p>
     * When a subscriber subscribes to the returned {@link Uni}, it subscribes to this {@link Multi} and requests one
     * item. The event emitted by this {@link Multi} are then forwarded to the {@link Uni}:
     *
     * <ul>
     * <li>on item event, the item is fired by the produced {@link Uni}</li>
     * <li>on failure event, the failure is fired by the produced {@link Uni}</li>
     * <li>on completion event, a {@code null} item is fired by the produces {@link Uni}</li>
     * <li>any item or failure events received after the first event is dropped</li>
     * </ul>
     * <p>
     * If the subscription on the produced {@link Uni} is cancelled, the subscription to the passed {@link Multi} is
     * also cancelled.
     *
     * @return the produced {@link Uni}
     */
    Uni<T> toUni();

    /**
     * Like {@link #onFailure(Predicate)} but applied to all failures fired by the upstream multi.
     * It allows configuring the on failure behavior (recovery, retry...).
     *
     * @return a MultiOnFailure on which you can specify the on failure action
     */
    MultiOnFailure<T> onFailure();

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
    MultiOnFailure<T> onFailure(Predicate<? super Throwable> predicate);

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
    MultiOnFailure<T> onFailure(Class<? extends Throwable> typeOfFailure);

    /**
     * Allows adding behavior when various type of events are emitted by the current {@link Multi} (item, failure,
     * completion) or by the subscriber (cancellation, request, subscription)
     *
     * @return the object to configure the action to execute when events happen
     */
    MultiOnEvent<T> on();

    /**
     * Creates a new {@link Multi} that subscribes to this upstream and caches all of its events and replays them, to
     * all the downstream subscribers.
     *
     * @return a multi replaying the events from the upstream.
     */
    Multi<T> cache();

    /**
     * Produces {@link Multi} or {@link Uni} collecting items from this {@link Multi}. You can accumulate the items
     * into a {@link java.util.List} ({@link MultiCollect#asList()}), {@link java.util.Map}
     * ({@link MultiCollect#asMap(Function)}...
     * <p>
     * You can also retrieve the first and list items using {@link MultiCollect#first()} and {@link MultiCollect#last()}.
     *
     * @return the object to configure the collection process.
     */
    MultiCollect<T> collect();

    /**
     * Produces {@link Multi} grouping items from this {@link Multi} into various "form of chunks" (list, {@link Multi}).
     * The grouping can be done linearly ({@link MultiGroup#intoLists()} and {@link MultiGroup#intoMultis()}, or based
     * on a grouping function ({@link MultiGroup#by(Function)})
     *
     * @return the object to configure the grouping.
     */
    MultiGroup<T> group();

    /**
     * Produces a new {@link Multi} invoking the {@code onItem}, {@code onFailure} and {@code onCompletion} methods
     * on the supplied {@link Executor}.
     * <p>
     * Instead of receiving the {@code item} event on the thread firing the event, this method  influences the
     * threading context to switch to a thread from the given executor. Same behavior for failure and completion.
     *
     * Note that the subscriber is guaranteed to never be called concurrently.
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link Multi}
     */
    Multi<T> emitOn(Executor executor);

    /**
     * When a subscriber subscribes to this {@link Multi}, execute the subscription to the upstream {@link Multi} on a
     * thread from the given executor. As a result, the {@link Subscriber#onSubscribe(Subscription)} method will be called
     * on this thread (except mentioned otherwise)
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link Multi}
     */
    Multi<T> subscribeOn(Executor executor);

    /**
     * Allows configures the actions or continuation to execute when this {@link Multi} fires the completion event.
     * @return the object to configure the action.
     */
    MultiOnCompletion<T> onCompletion();

    /**
     * Transforms the streams by skipping, selecting, or merging.
     * @return the object to configure the transformation.
     */
    MultiTransform<T> transform();
}
