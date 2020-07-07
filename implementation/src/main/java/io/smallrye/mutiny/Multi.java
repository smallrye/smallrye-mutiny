package io.smallrye.mutiny;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.groups.MultiBroadcast;
import io.smallrye.mutiny.groups.MultiCollect;
import io.smallrye.mutiny.groups.MultiConvert;
import io.smallrye.mutiny.groups.MultiCreate;
import io.smallrye.mutiny.groups.MultiCreateBy;
import io.smallrye.mutiny.groups.MultiGroup;
import io.smallrye.mutiny.groups.MultiOnCompletion;
import io.smallrye.mutiny.groups.MultiOnEvent;
import io.smallrye.mutiny.groups.MultiOnFailure;
import io.smallrye.mutiny.groups.MultiOnItem;
import io.smallrye.mutiny.groups.MultiOnSubscribe;
import io.smallrye.mutiny.groups.MultiOverflow;
import io.smallrye.mutiny.groups.MultiSubscribe;
import io.smallrye.mutiny.groups.MultiTransform;

@SuppressWarnings("PublisherImplementation")
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

    /**
     * Configures the subscriber consuming this {@link Multi}.
     *
     * @return the object to configure the subscriber
     */
    MultiSubscribe<T> subscribe();

    /**
     * Configures the behavior when an {@code item} event is received from the this {@link Multi}
     *
     * @return the object to configure the behavior.
     */
    MultiOnItem<T> onItem();

    /**
     * Allows structuring the pipeline by creating a logic separation:
     *
     * <pre>
     * {@code
     *     Multi multi = upstream
     *      .then(m -> { ...})
     *      .then(m -> { ...})
     *      .then(m -> { ...})
     * }
     * </pre>
     * <p>
     * With `then` you can structure and chain groups of processing.
     *
     * @param stage the function receiving this {@link Multi} as parameter and producing the outcome (can be a
     *        {@link Multi} or something else), must not be {@code null}.
     * @param <O> the outcome type
     * @return the outcome of the function.
     * @deprecated use {@link #stage(Function)}
     */
    @Deprecated
    default <O> O then(Function<Multi<T>, O> stage) {
        return stage(stage);
    }

    /**
     * Allows structuring the pipeline by creating a logic separation:
     *
     * <pre>
     * {@code
     *     Multi multi = upstream
     *      .stage(m -> { ...})
     *      .stage(m -> { ...})
     *      .stage(m -> { ...})
     * }
     * </pre>
     * <p>
     * With `stage` you can structure and chain groups of processing.
     *
     * @param stage the function receiving this {@link Multi} as parameter and producing the outcome (can be a
     *        {@link Multi} or something else), must not be {@code null}.
     * @param <O> the outcome type
     * @return the outcome of the function.
     */
    default <O> O stage(Function<Multi<T>, O> stage) {
        return nonNull(stage, "stage").apply(this);
    }

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
     * Configures the action to execute when the observed {@link Multi} sends a {@link Subscription}.
     * The downstream don't have a subscription yet. It will be passed once the configured action completes.
     *
     * For example:
     *
     * <pre>
     * {@code
     * multi.onSubscribe().invoke(sub -> System.out.println("subscribed"));
     * // Delay the subscription by 1 second (or until an asynchronous action completes)
     * multi.onSubscribe().invokeUni(sub -> Uni.createFrom(1).onItem().delayIt().by(Duration.ofSecond(1)));
     * }
     * </pre>
     *
     * @return the object to configure the action to execution on subscription.
     */
    MultiOnSubscribe<T> onSubscribe();

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
    MultiCollect<T> collectItems();

    /**
     * Produces {@link Multi} grouping items from this {@link Multi} into various "form of chunks" (list, {@link Multi}).
     * The grouping can be done linearly ({@link MultiGroup#intoLists()} and {@link MultiGroup#intoMultis()}, or based
     * on a grouping function ({@link MultiGroup#by(Function)})
     *
     * @return the object to configure the grouping.
     */
    MultiGroup<T> groupItems();

    /**
     * Produces a new {@link Multi} invoking the {@code onItem}, {@code onFailure} and {@code onCompletion} methods
     * on the supplied {@link Executor}.
     * <p>
     * Instead of receiving the {@code item} event on the thread firing the event, this method influences the
     * threading context to switch to a thread from the given executor. Same behavior for failure and completion.
     * <p>
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
     * @deprecated Use {@link #runSubscriptionOn(Executor)} instead.
     */
    @Deprecated
    default Multi<T> subscribeOn(Executor executor) {
        return runSubscriptionOn(executor);
    }

    /**
     * When a subscriber subscribes to this {@link Multi}, execute the subscription to the upstream {@link Multi} on a
     * thread from the given executor. As a result, the {@link Subscriber#onSubscribe(Subscription)} method will be called
     * on this thread (except mentioned otherwise)
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link Multi}
     */
    Multi<T> runSubscriptionOn(Executor executor);

    /**
     * Allows configures the actions or continuation to execute when this {@link Multi} fires the completion event.
     *
     * @return the object to configure the action.
     */
    MultiOnCompletion<T> onCompletion();

    /**
     * Transforms the streams by skipping, selecting, or merging.
     *
     * @return the object to configure the transformation.
     */
    MultiTransform<T> transform();

    /**
     * Configures the back-pressure behavior when the consumer cannot keep up with the emissions from this
     * {@link Multi}.
     *
     * @return the object to configure the overflow strategy
     */
    MultiOverflow<T> onOverflow();

    /**
     * Makes this {@link Multi} be able to broadcast its events ({@code items}, {@code failure}, and {@code completion})
     * to multiple subscribers.
     *
     * @return the object to configure the broadcast
     */
    MultiBroadcast<T> broadcast();

    /**
     * Converts a {@link Multi} to other types
     *
     * <p>
     * Examples:
     * </p>
     *
     * <pre>
     * {@code
     * multi.convert().with(multi -> x); // Convert with a custom lambda converter
     * }
     * </pre>
     *
     * @return the object to convert an {@link Multi} instance
     * @see MultiConvert
     */
    MultiConvert<T> convert();

    /**
     * Produces a new {@link Multi} with items from the upstream {@link Multi} matching the predicate.
     * <p>
     * Items that do not satisfy the predicate are discarded.
     * <p>
     * This method is a shortcut for {@code multi.transform().byFilteringItemsWith(predicate)}.
     *
     * @param predicate a predicate, must not be {@code null}
     * @return the new {@link Multi}
     */
    default Multi<T> filter(Predicate<? super T> predicate) {
        return transform().byFilteringItemsWith(predicate);
    }

    /**
     * Produces a new {@link Multi} invoking the given function for each item emitted by the upstream {@link Multi}.
     * <p>
     * The function receives the received item as parameter, and can transform it. The returned object is sent
     * downstream as {@code item} event.
     * <p>
     * This method is a shortcut for {@code multi.onItem().apply(mapper)}.
     *
     * @param mapper the mapper function, must not be {@code null}
     * @param <O> the type of item produced by the mapper function
     * @return the new {@link Multi}
     */
    default <O> Multi<O> map(Function<? super T, ? extends O> mapper) {
        return onItem().transform(nonNull(mapper, "mapper"));
    }

    /**
     * Produces a {@link Multi} containing the items from {@link Publisher} produced by the {@code mapper} for each
     * item emitted by this {@link Multi}.
     * <p>
     * The operation behaves as follows:
     * <ul>
     * <li>for each item emitted by this {@link Multi}, the mapper is called and produces a {@link Publisher}
     * (potentially a {@code Multi}). The mapper must not return {@code null}</li>
     * <li>The items emitted by each of the produced {@link Publisher} are then <strong>merged</strong> in the
     * produced {@link Multi}. The flatten process may interleaved items.</li>
     * </ul>
     * This method is a shortcut for {@code multi.onItem().transformToMulti(mapper).merge()}.
     *
     * @param mapper the {@link Function} producing {@link Publisher} / {@link Multi} for each items emitted by the
     *        upstream {@link Multi}
     * @param <O> the type of item emitted by the {@link Publisher} produced by the {@code mapper}
     * @return the produced {@link Multi}
     */
    default <O> Multi<O> flatMap(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return onItem().transformToMulti(mapper).merge();
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when an {@code item} event is fired by the upstream.
     * Note that the received item cannot be {@code null}.
     * <p>
     * If the callback throws an exception, this exception is propagated to the downstream as failure. No more items
     * will be consumed.
     * <p>
     * This method is a shortcut on {@link MultiOnItem#invoke(Consumer)}.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Multi}
     */
    default Multi<T> invoke(Consumer<? super T> callback) {
        return onItem().invoke(nonNull(callback, "callback"));
    }

    /**
     * Produces a new {@link Multi} invoking the given @{code action} when an {@code item} event is received. Note that
     * the received item cannot be {@code null}.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its result, the result is discarded, and the original {@code item} is forwarded downstream. If the produced
     * {@code Uni} fails, the failure is propagated downstream. If the action throws an exception, this exception is
     * propagated downstream as failure.
     * <p>
     * This method preserves the order of the items, meaning that the downstream received the items in the same order
     * as the upstream has emitted them.
     *
     * @param action the function taking the item and returning a {@link Uni}, must not be {@code null}, must not return
     *        {@code null}
     * @return the new {@link Multi}
     */
    default Multi<T> invokeUni(Function<? super T, ? extends Uni<?>> action) {
        return onItem().invokeUni(nonNull(action, "action"));
    }

    /**
     * Produces a {@link Multi} containing the items from {@link Publisher} produced by the {@code mapper} for each
     * item emitted by this {@link Multi}.
     * <p>
     * The operation behaves as follows:
     * <ul>
     * <li>for each item emitted by this {@link Multi}, the mapper is called and produces a {@link Publisher}
     * (potentially a {@code Multi}). The mapper must not return {@code null}</li>
     * <li>The items emitted by each of the produced {@link Publisher} are then <strong>concatenated</strong> in the
     * produced {@link Multi}. The flatten process makes sure that the items are not interleaved.
     * </ul>
     * <p>
     * This method is equivalent to {@code multi.onItem().transformToMulti(mapper).concatenate()}.
     *
     * @param mapper the {@link Function} producing {@link Publisher} / {@link Multi} for each items emitted by the
     *        upstream {@link Multi}
     * @param <O> the type of item emitted by the {@link Publisher} produced by the {@code mapper}
     * @return the produced {@link Multi}
     */
    default <O> Multi<O> concatMap(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return onItem().transformToMulti(mapper).concatenate();
    }
}
