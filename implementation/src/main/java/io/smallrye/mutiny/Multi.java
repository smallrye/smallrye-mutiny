package io.smallrye.mutiny;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.*;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.groups.*;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.split.MultiSplitter;

public interface Multi<T> extends Publisher<T> {

    @CheckReturnValue
    static MultiCreate createFrom() {
        return MultiCreate.INSTANCE;
    }

    /**
     * Creates new instances of {@link Multi} by merging, concatenating or associating items from others {@link Multi}
     * and {@link Publisher}.
     *
     * @return the object to configure the creation process.
     */
    @CheckReturnValue
    static MultiCreateBy createBy() {
        return MultiCreateBy.INSTANCE;
    }

    /**
     * Configures the subscriber consuming this {@link Multi}.
     *
     * @return the object to configure the subscriber
     */
    @CheckReturnValue
    MultiSubscribe<T> subscribe();

    /**
     * Configures the behavior when an {@code item} event is received from the this {@link Multi}
     *
     * @return the object to configure the behavior.
     */
    @CheckReturnValue
    MultiOnItem<T> onItem();

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
    @CheckReturnValue
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
    @CheckReturnValue
    Uni<T> toUni();

    /**
     * Like {@link #onFailure(Predicate)} but applied to all failures fired by the upstream multi.
     * It allows configuring the on failure behavior (recovery, retry...).
     *
     * @return a MultiOnFailure on which you can specify the on failure action
     */
    @CheckReturnValue
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
    @CheckReturnValue
    MultiOnFailure<T> onFailure(Predicate<? super Throwable> predicate);

    /**
     * Configures the action to execute when the observed {@link Multi} sends a {@link Subscription}.
     * The downstream does not have a subscription yet. It will be passed once the configured action completes.
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
     * @return the object to configure the action to execution on subscription.
     */
    @CheckReturnValue
    MultiOnSubscribe<T> onSubscription();

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
    @CheckReturnValue
    MultiOnFailure<T> onFailure(Class<? extends Throwable> typeOfFailure);

    /**
     * Produces a {@link Multi} reacting when no item event is fired by the upstream multi for the specified length
     * of time.
     * <p>
     * This {@link Multi} detects if this {@link Multi} does not emit an item for the configured length of time.
     * <p>
     * Examples:
     * <code>
     * multi.ifNoItem().after(Duration.ofMillis(1000)).fail() // Propagate a TimeoutException
     * multi.ifNoItem().after(Duration.ofMillis(1000)).recoverWithCompletion() // Complete the event on timeout
     * multi.ifNoItem().after(Duration.ofMillis(1000)).on(myExecutor)... // Configure the executor calling on timeout actions
     * </code>
     *
     * @return the on item timeout group
     */
    @CheckReturnValue
    MultiIfNoItem<T> ifNoItem();

    /**
     * Creates a new {@link Multi} that subscribes to this upstream and caches all of its events and replays them, to
     * all the downstream subscribers.
     *
     * @return a multi replaying the events from the upstream.
     */
    @CheckReturnValue
    Multi<T> cache();

    /**
     * Produces {@link Uni} collecting/aggregating items from this {@link Multi}.
     * It allows accumulating the items emitted by this {@code multi} into a structure such as a into a
     * {@link java.util.List} ({@link MultiCollect#asList()}), a {@link java.util.Map}
     * ({@link MultiCollect#asMap(Function)}, or a custom collector.
     * When this {@code multi} sends the completion signal, the structure is emitted by the returned {@link Uni}.
     * <p>
     * If this {@link Multi} emits a failure, the produced {@link Uni} produces the same failure and the aggregated items
     * are discarded.
     * <p>
     * You can also retrieve the first and last items using {@link MultiCollect#first()} and {@link MultiCollect#last()}.
     * Be aware to not used method collecting items on unbounded / infinite {@link Multi}.
     *
     * @return the object to configure the collection process.
     */
    @CheckReturnValue
    MultiCollect<T> collect();

    /**
     * Produces {@link Multi} grouping items from this {@link Multi} into various "form of chunks" (list, {@link Multi}).
     * The grouping can be done linearly ({@link MultiGroup#intoLists()} and {@link MultiGroup#intoMultis()}, or based
     * on a grouping function ({@link MultiGroup#by(Function)})
     *
     * @return the object to configure the grouping.
     */
    @CheckReturnValue
    MultiGroup<T> group();

    /**
     * Produces a new {@link Multi} invoking the {@code onItem}, {@code onFailure} and {@code onCompletion} methods
     * on the supplied {@link Executor}.
     * <p>
     * Instead of receiving the {@code item} event on the thread firing the event, this method influences the
     * threading context to switch to a thread from the given executor. Same behavior for failure and completion.
     * <p>
     * Note that the subscriber is guaranteed to never be called concurrently.
     * <p>
     * <strong>Be careful as this operator can lead to concurrency problems with non thread-safe objects such as
     * CDI request-scoped beans.
     * It might also break reactive-streams semantics with items being emitted concurrently.</strong>
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link Multi}
     */
    @CheckReturnValue
    Multi<T> emitOn(Executor executor);

    /**
     * When a subscriber subscribes to this {@link Multi}, execute the subscription to the upstream {@link Multi} on a
     * thread from the given executor. As a result, the {@link Subscriber#onSubscribe(Subscription)} method will be called
     * on this thread (except mentioned otherwise)
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link Multi}
     */
    @CheckReturnValue
    Multi<T> runSubscriptionOn(Executor executor);

    /**
     * Allows configuring the actions or continuation to execute when this {@link Multi} fires the completion event.
     *
     * @return the object to configure the action.
     */
    @CheckReturnValue
    MultiOnCompletion<T> onCompletion();

    /**
     * Selects items from this {@link Multi}.
     *
     * @return the object to configure the selection.
     */
    @CheckReturnValue
    MultiSelect<T> select();

    /**
     * Skips items from this {@link Multi}.
     *
     * @return the object to configure the skip.
     */
    @CheckReturnValue
    MultiSkip<T> skip();

    /**
     * Configures the back-pressure behavior when the consumer cannot keep up with the emissions from this
     * {@link Multi}.
     *
     * @return the object to configure the overflow strategy
     */
    @CheckReturnValue
    MultiOverflow<T> onOverflow();

    /**
     * Makes this {@link Multi} be able to broadcast its events ({@code items}, {@code failure}, and {@code completion})
     * to multiple subscribers.
     *
     * @return the object to configure the broadcast
     */
    @CheckReturnValue
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
    @CheckReturnValue
    MultiConvert<T> convert();

    /**
     * Produces a new {@link Multi} with items from the upstream {@link Multi} matching the predicate.
     * <p>
     * Items that do not satisfy the predicate are discarded.
     * <p>
     * This method is a shortcut for {@code multi.select().where(predicate)}.
     *
     * @param predicate a predicate, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    default Multi<T> filter(Predicate<? super T> predicate) {
        return select().where(predicate);
    }

    /**
     * Produces a new {@link Multi} invoking the given function for each item emitted by the upstream {@link Multi}.
     * <p>
     * The function receives the received item as parameter, and can transform it. The returned object is sent
     * downstream as {@code item} event.
     * <p>
     * This method is a shortcut for {@code multi.onItem().transform(mapper)}.
     *
     * @param mapper the mapper function, must not be {@code null}
     * @param <O> the type of item produced by the mapper function
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    default <O> Multi<O> map(Function<? super T, ? extends O> mapper) {
        return onItem().transform(mapper);
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
    @CheckReturnValue
    default <O> Multi<O> flatMap(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return onItem().transformToMultiAndMerge(mapper);
    }

    /**
     * Produces a new {@link Multi} invoking the given @{code action} when an {@code item} event is received. Note that
     * the received item cannot be {@code null}.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its result, the result is discarded, and the original {@code item} is forwarded downstream. If the produced
     * {@code Uni} fails, the failure is propagated downstream.
     * <p>
     * If the asynchronous action throws an exception, this exception is propagated downstream.
     * <p>
     * This method preserves the order of the items, meaning that the downstream received the items in the same order
     * as the upstream has emitted them.
     *
     * @param action the function taking the item and returning a {@link Uni}, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    default Multi<T> call(Function<? super T, Uni<?>> action) {
        return onItem().call(action);
    }

    /**
     * Produces a new {@link Multi} invoking the given @{code action} when an {@code item} event is received, but
     * ignoring it in the callback.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its result, the result is discarded, and the original {@code item} is forwarded downstream. If the produced
     * {@code Uni} fails, the failure is propagated downstream.
     * <p>
     * If the asynchronous action throws an exception, this exception is propagated downstream.
     * <p>
     * This method preserves the order of the items, meaning that the downstream received the items in the same order
     * as the upstream has emitted them.
     *
     * @param action the function taking the item and returning a {@link Uni}, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    default Multi<T> call(Supplier<Uni<?>> action) {
        return onItem().call(action);
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
    @CheckReturnValue
    default Multi<T> invoke(Consumer<? super T> callback) {
        return onItem().invoke(nonNull(callback, "callback"));
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when an {@code item} event is fired by the upstream.
     * <p>
     * If the callback throws an exception, this exception is propagated to the downstream as failure. No more items
     * will be consumed.
     * <p>
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    default Multi<T> invoke(Runnable callback) {
        return onItem().invoke(callback);
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
     * This method is equivalent to {@code multi.onItem().transformToMulti(mapper).concatenate(true)}.
     *
     * @param mapper the {@link Function} producing {@link Publisher} / {@link Multi} for each items emitted by the
     *        upstream {@link Multi}
     * @param <O> the type of item emitted by the {@link Publisher} produced by the {@code mapper}
     * @return the produced {@link Multi}
     */
    @CheckReturnValue
    default <O> Multi<O> concatMap(Function<? super T, ? extends Publisher<? extends O>> mapper) {
        return onItem().transformToMultiAndConcatenate(mapper);
    }

    /**
     * Configures actions when this {@link Multi} terminates on completion, on failure or on subscriber cancellation.
     *
     * @return the object to configure the termination actions
     */
    @CheckReturnValue
    MultiOnTerminate<T> onTermination();

    /**
     * Configures actions when the subscriber cancels the subscription.
     *
     * @return the object to configure the cancellation actions
     */
    @CheckReturnValue
    MultiOnCancel<T> onCancellation();

    /**
     * Configures actions when items are being requested.
     *
     * @return the object to configure the actions
     */
    @CheckReturnValue
    MultiOnRequest<T> onRequest();

    /**
     * Plug a user-defined operator that does not belong to the existing Mutiny API.
     *
     * @param operatorProvider a function to create and bind a new operator instance, taking {@code this} {@link Multi} as a
     *        parameter and returning a new {@link Multi}. Must neither be {@code null} nor return {@code null}.
     * @param <R> the output type
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    default <R> Multi<R> plug(Function<Multi<T>, Multi<R>> operatorProvider) {
        Function<Multi<T>, Multi<R>> provider = nonNull(operatorProvider, "operatorProvider");
        return Infrastructure.onMultiCreation(nonNull(provider.apply(this), "multi"));
    }

    /**
     * Produces a new {@link Multi} transforming this {@code Multi} into a hot stream.
     * <p>
     * With a hot stream, when no subscribers are present, emitted items are dropped.
     * Late subscribers would only receive items emitted after their subscription.
     * If the upstream has already been terminated, the termination event (failure or completion) is forwarded to the
     * subscribers.
     * <p>
     * Note that this operator consumes the upstream stream without back-pressure.
     * It still enforces downstream back-pressure.
     * If the subscriber is not ready to receive an item when the upstream emits an item, the subscriber gets a
     * {@link io.smallrye.mutiny.subscription.BackPressureFailure} failure.
     *
     * @return the new multi.
     */
    @CheckReturnValue
    Multi<T> toHotStream();

    /**
     * Log events (onSubscribe, onItem, ...) as they come from the upstream or the subscriber.
     * <p>
     * Events will be logged as long as the {@link Multi} hasn't been cancelled or terminated.
     * Logging is framework-agnostic and can be configured in the {@link Infrastructure} class.
     *
     * @param identifier an identifier of this operator to be used in log events
     * @return a new {@link Multi}
     * @see Infrastructure#setOperatorLogger(Infrastructure.OperatorLogger)
     */
    @CheckReturnValue
    Multi<T> log(String identifier);

    /**
     * Log events (onSubscribe, onItem, ...) as they come from the upstream or the subscriber, and derives the identifier from
     * the upstream operator class "simple name".
     * <p>
     * Events will be logged as long as the {@link Multi} hasn't been cancelled or terminated.
     * Logging is framework-agnostic and can be configured in the {@link Infrastructure} class.
     *
     * @return a new {@link Multi}
     * @see Multi#log(String)
     * @see Infrastructure#setOperatorLogger(Infrastructure.OperatorLogger)
     */
    @CheckReturnValue
    Multi<T> log();

    /**
     * Materialize the subscriber {@link Context} for a sub-pipeline.
     *
     * <p>
     * The provided function takes this {@link Multi} and the {@link Context} as parameters, and returns a {@link Multi}
     * to build the sub-pipeline, as in:
     *
     * <pre>
     * {@code
     * someMulti.withContext((multi, ctx) -> multi.onItem().transform(n -> n + "::" + ctx.getOrElse("foo", () -> "yolo")));
     * }
     * </pre>
     *
     * <p>
     * Note that the {@code builder} function is called <strong>at subscription time</strong>, so it cannot see
     * context updates from upstream operators yet.
     *
     * @param builder the function that builds the sub-pipeline from this {@link Multi} and the {@link Context},
     *        must not be {@code null}, must not return {@code null}.
     * @param <R> the resulting {@link Multi} type
     * @return the resulting {@link Multi}
     */
    @CheckReturnValue
    default <R> Multi<R> withContext(BiFunction<Multi<T>, Context, Multi<R>> builder) {
        throw new UnsupportedOperationException("Default method added to limit binary incompatibility");
    }

    /**
     * Materialize the context by attaching it to items using the {@link ItemWithContext} wrapper class.
     *
     * <p>
     * This is a shortcut for:
     *
     * <pre>
     * {@code
     * someMulti.withContext((multi, ctx) -> multi.onItem().transform(item -> new ItemWithContext<>(ctx, item)));
     * }
     * </pre>
     *
     * @return the resulting {@link Multi}
     * @see #withContext(BiFunction)
     */
    @CheckReturnValue
    default Multi<ItemWithContext<T>> attachContext() {
        return this.withContext((multi, ctx) -> multi.onItem().transform(item -> new ItemWithContext<>(ctx, item)));
    }

    /**
     * A demand-pacer allows controlling upstream demand using a request and a delay.
     * <p>
     * Each time the delay expires the pacer can evaluate a new demand based on the previous request and the number of emitted
     * items that have been observed since the previous request.
     * <p>
     * In the following example a demand of 25 is issued every 100ms, using the default worker pool to perform requests:
     *
     * <pre>
     * var pacer = new FixedDemandPacer(25L, Duration.ofMillis(100L));
     * var multi = Multi.createFrom().range(0, 100)
     *         .paceDemand().on(Infrastructure.getDefaultWorkerPool()).using(pacer);
     * </pre>
     *
     * <strong>Important: this operator is NOT compliant with the reactive streams specification.</strong>
     * Downstream demand requests are being ignored, so it is possible that this operator requests more than what the downstream
     * subscriber would want, depending on the {@link io.smallrye.mutiny.subscription.DemandPacer}
     * object in use.
     *
     * @return a group to configure the demand pacing
     */
    @CheckReturnValue
    MultiDemandPacing<T> paceDemand();

    /**
     * Cap all downstream subscriber requests to a maximum value.
     * <p>
     * This is a shortcut for:
     *
     * <pre>
     * multi.capDemandsUsing(outstanding -&gt; Math.min(outstanding, actual))
     * </pre>
     *
     * @param max the maximum demand
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    default Multi<T> capDemandsTo(long max) {
        long actual = positive(max, "max");
        return capDemandsUsing(outstanding -> Math.min(outstanding, actual));
    }

    /**
     * Cap all downstream subscriber requests to a value computed by a function.
     * <p>
     * The function must return a valid demand which is strictly positive and below or equal to that of the current outstanding
     * demand.
     * The function argument is the current outstanding demand.
     *
     * @param function the function, must not be {@code null}, must not return {@code null}, must return a {@code long} such
     *        that
     *        {@code (0 < n <= outstanding)} where {@code outstanding} is the current outstanding demand
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    Multi<T> capDemandsUsing(LongFunction<Long> function);

    /**
     * Splits this {@link Multi} into several co-operating {@link Multi} based on an enumeration and a mapping function.
     * <p>
     * Here is a sample where a stream of integers is split into streams for odd and even numbers:
     *
     * <pre>
     * {@code
     * // Split someMulti into 2 streams
     * var splitter = someMulti.split(OddEven.class, n -> (n % 2 == 0) ? OddEven.EVEN : OddEven.ODD);
     *
     * // Stream for odd numbers
     * vor odd = splitter.get(OddEven.ODD).subscribe().with(...);
     *
     * // Stream for even numbers
     * vor even = splitter.get(OddEven.EVEN).subscribe().with(...);
     * }
     * </pre>
     *
     * @param keyType the key type
     * @param splitter the splitter function
     * @return a splitter
     * @param <K> the key type
     */
    @CheckReturnValue
    @Experimental("Multi splitting is an experimental API in Mutiny 2.3.0")
    default <K extends Enum<K>> MultiSplitter<T, K> split(Class<K> keyType, Function<T, K> splitter) {
        return new MultiSplitter<>(this, keyType, splitter);
    }
}
