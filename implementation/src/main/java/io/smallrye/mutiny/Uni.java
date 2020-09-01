package io.smallrye.mutiny;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.*;

import io.smallrye.mutiny.groups.*;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.smallrye.mutiny.tuples.Tuple;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.tuples.Tuple9;

/**
 * A {@link Uni} represents a lazy asynchronous action. It follows the subscription pattern, meaning that the action
 * is only triggered once a {@link UniSubscriber} subscribes to the {@link Uni}.
 * <p>
 * A {@link Uni} can have two outcomes:
 * <ol>
 * <li>An {@code item} event, forwarding the completion of the action (potentially {@code null} if the item
 * does not represent a value, but the action was completed successfully)</li>
 * <li>A {@code failure} event, forwarding an exception</li>
 * </ol>
 * <p>
 * To trigger the computation, a {@link UniSubscriber} must subscribe to the Uni. It will be notified of the outcome
 * once there is an {@code item} or {@code failure} event fired by the observed Uni. A subscriber receives
 * (asynchronously) a {@link UniSubscription} and can cancel the demand at any time. Note that cancelling after
 * having received the outcome is a no-op.
 * <p>
 *
 * @param <T> the type of item produced by the {@link Uni}
 */
public interface Uni<T> {

    /**
     * Creates a new {@link Uni} from various sources such as {@link CompletionStage},
     * {@link UniEmitter}, direct values, {@link Exception}...
     *
     * <p>
     * Examples:
     * </p>
     *
     * <pre>
     * {@code
     * Uni.createFrom().item(1); // Emit 1 at subscription time
     * Uni.createFrom().item(() -> x); // Emit x at subscription time, the supplier is invoked for each subscription
     * Uni.createFrom().completionStage(cs); // Emit the item from this completion stage
     * Uni.createFrom().completionStage(() -> cs); // Emit the item from this completion stage, the stage is not created before subscription
     * Uni.createFrom().failure(exception); // Emit the failure at subscription time
     * Uni.createFrom().deferred(() -> Uni.from().value(x)); // Defer the uni creation until subscription. Each subscription can produce a different uni
     * Uni.createFrom().item(null); // Emit null at subscription time
     * Uni.createFrom().nothing(); // Create a Uni not emitting any signal
     * Uni.createFrom().publisher(publisher); // Create a Uni from a Reactive Streams Publisher
     * }
     * </pre>
     *
     * @return the factory used to create {@link Uni} instances.
     * @see UniCreate
     */
    static UniCreate createFrom() {
        return UniCreate.INSTANCE;
    }

    /**
     * Allows structuring the pipeline by creating a logic separation:
     *
     * <pre>
     * {@code
     *     Uni uni = upstream
     *      .then(u -> { ...})
     *      .then(u -> { ...})
     *      .then(u -> { ...})
     * }
     * </pre>
     * <p>
     * With `then` you can structure and chain groups of processing.
     *
     * @param stage the function receiving the this {@link Uni} as parameter and producing the outcome (can be a
     *        {@link Uni} or something else), must not be {@code null}.
     * @param <O> the outcome type
     * @return the outcome of the function.
     * @deprecated use {@link #stage(Function)}
     */
    @Deprecated
    default <O> O then(Function<Uni<T>, O> stage) {
        return stage(stage);
    }

    /**
     * Allows structuring the pipeline by creating a logic separation:
     *
     * <pre>
     * {@code
     *     Uni uni = upstream
     *      .stage(u -> { ...})
     *      .stage(u -> { ...})
     *      .stage(u -> { ...})
     * }
     * </pre>
     * <p>
     * With `stage` you can structure and chain groups of processing.
     *
     * @param stage the function receiving the this {@link Uni} as parameter and producing the outcome (can be a
     *        {@link Uni} or something else), must not be {@code null}.
     * @param <O> the outcome type
     * @return the outcome of the function.
     */
    default <O> O stage(Function<Uni<T>, O> stage) {
        return nonNull(stage, "stage").apply(this);
    }

    /**
     * Creates a new {@link Uni} combining several others unis such as {@code all} or {@code any}.
     *
     * @return the factory use to combine the uni instances
     */
    static UniCombine combine() {
        return UniCombine.INSTANCE;
    }

    /**
     * Requests the {@link Uni} to start resolving the item and allows configuring how the signals are propagated
     * (using a {@link UniSubscriber}, callbacks, or a {@link CompletionStage}. Unlike {@link #await()}, this method
     * configures non-blocking retrieval of the item and failure.
     *
     * <p>
     * Examples:
     * </p>
     *
     * <pre>
     * {@code
     *     Uni<String> uni = ...;
     *
     *    Subscription sub = uni.subscribe().with( // The return subscription can be used to cancel the operation
     *              item -> {},           // Callback calls on item
     *              failure -> {}           // Callback calls on failure
     *    );
     *
     *    UniSubscriber<String> myUniSubscriber = ...
     *    uni.subscribe().withSubscriber(myUniSubscriber); // Subscribes to the Uni with the passed subscriber
     *
     *    CompletableFuture future = uni.subscribe().asCompletableFuture(); // Get a CompletionStage receiving the item or failure
     *    // Cancelling the returned future cancels the subscription.
     * }
     * </pre>
     *
     * @return the object to configure the subscription.
     * @see #await() <code>uni.await() </code>for waiting (blocking the caller thread) until the resolution of the observed Uni.
     */
    UniSubscribe<T> subscribe();

    /**
     * Shortcut for {@link UniSubscribe#asCompletionStage()}.
     *
     * @return the completion stage receiving the items emitted by this {@link Uni}
     */
    default CompletableFuture<T> subscribeAsCompletionStage() {
        return subscribe().asCompletionStage();
    }

    /**
     * Awaits (blocking the caller thread) until the item or a failure is emitted by the observed {@link Uni}.
     * If the observed uni fails, the failure is thrown. In the case of a checked exception, the exception is wrapped
     * into a {@link java.util.concurrent.CompletionException}.
     *
     * <p>
     * Examples:
     * </p>
     *
     * <pre>
     * {@code
     * Uni<T> uni = ...;
     * T res = uni.await().indefinitely(); // Await indefinitely until it get the item.
     * T res = uni.await().atMost(Duration.ofMillis(1000)); // Awaits at most 1s. After that, a TimeoutException is thrown
     * Optional<T> res = uni.await().asOptional().indefinitely(); // Retrieves the item as an Optional, empty if the item is null
     * }
     * </pre>
     *
     * @return the object to configure the retrieval.
     */
    UniAwait<T> await();

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
     */
    UniOnItem<T> onItem();

    /**
     * Configures the action to execute when the observed {@link Uni} sends a {@link UniSubscription}.
     * The downstream don't have a subscription yet. It will be passed once the configured action completes.
     *
     * <p>
     * Example:
     * </p>
     *
     * <pre>
     * {@code
     * uni.onSubscribe().invoke(sub -> System.out.println("subscribed"));
     * // Delay the subscription by 1 second (or until an asynchronous action completes)
     * uni.onSubscribe().invokeUni(sub -> Uni.createFrom(1).onItem().delayIt().by(Duration.ofSecond(1)));
     * }
     * </pre>
     *
     * @return the object to configure the action to execution on subscription.
     */
    UniOnSubscribe<T> onSubscribe();

    /**
     * Configures the action to execute when the observed {@link Uni} emits either an item (potentially {@code null}))
     * or a failure. Unlike {@link #onItem()} and {@link #onFailure()} the action would handle both cases in on "go".
     *
     * @return the object to configure the action to execute when an item is emitted or when a failure is propagated.
     */
    UniOnItemOrFailure<T> onItemOrFailure();

    /**
     * Combines a set of {@link Uni unis} into a joined item. This item can be a {@code Tuple} or the item of a
     * combinator function.
     * <p>
     * If one of the combine {@link Uni} fire a failure, the other unis are cancelled, and the resulting
     * {@link Uni} fires the failure. If {@code collectFailures()} is called,
     * it waits for the completion of all the {@link Uni unis} before propagating the failure event. If more than one
     * {@link Uni} failed, a {@link CompositeException} is fired, wrapping the different collected failures.
     * <p>
     * Depending on the number of participants, the produced {@link Tuple} is
     * different from {@link Tuple2} to {@link Tuple9}. For more participants,
     * use {@link UniAndGroup#unis(Uni[])} or
     * {@link UniAndGroup#unis(Iterable)}.
     *
     * @return the object to configure the join
     * @see UniCombine#all() <code>Uni.all()</code> for the equivalent static operator
     * @deprecated Use {@link #combine()}
     */
    @Deprecated
    UniAndGroup<T> and();

    /**
     * Combines the item of this {@link Uni} with the item of {@code other} into a {@link Tuple2}.
     * If {@code this} or {@code other} fails, the other resolution is cancelled.
     *
     * @param other the other {@link Uni}, must not be {@code null}
     * @param <T2> the type to pair
     * @return the combination of the pair combining the two items.
     * @see #and() <code>and</code> for more options on the combination of items
     * @see UniCombine#all() <code>Uni.all()</code> for the equivalent static operator
     * @deprecated Use {@link #combine()}
     */
    @Deprecated
    <T2> Uni<Tuple2<T, T2>> and(Uni<T2> other);

    /**
     * Composes this {@link Uni} with a set of {@link Uni} passed to
     * {@link UniOr#unis(Uni[])} to produce a new {@link Uni} forwarding the first event
     * (item or failure). It behaves like the fastest of these competing unis.
     * <p>
     * The process subscribes to the set of {@link Uni}. When one of the {@link Uni} fires an item or a failure,
     * the event is propagated downstream. Also the other subscriptions are cancelled.
     * <p>
     * Note that the callback from the subscriber are called on the thread used to fire the winning {@link Uni}.
     * Use {@link #emitOn(Executor)} to change the thread.
     * <p>
     * If the subscription to the returned {@link Uni} is cancelled, the subscription to the {@link Uni unis} from the
     * {@code iterable} are also cancelled.
     *
     * @return the object to enlist the participants
     * @see UniCombine#any() <code>Uni.any</code> for a static version of this operator, like
     *      <code>Uni first = Uni.any().of(uni1, uni2);</code>
     * @deprecated Use {@link #combine()}
     */
    @Deprecated
    UniOr<T> or();

    /**
     * Like {@link #onFailure(Predicate)} but applied to all failures fired by the upstream uni.
     * It allows configuring the on failure behavior (recovery, retry...).
     *
     * @return a UniOnFailure on which you can specify the on failure action
     */
    UniOnFailure<T> onFailure();

    /**
     * Configures a predicate filtering the failures on which the behavior (specified with the returned
     * {@link UniOnFailure}) is applied.
     * <p>
     * For instance, to only when an {@code IOException} is fired as failure you can use:
     * <code>uni.onFailure(IOException.class).recoverWithItem("hello")</code>
     * <p>
     * The fallback value ({@code hello}) will only be used if the upstream uni fire a failure of type
     * {@code IOException}.
     *
     * @param predicate the predicate, {@code null} means applied to all failures
     * @return a UniOnFailure configured with the given predicate on which you can specify the on failure action
     */
    UniOnFailure<T> onFailure(Predicate<? super Throwable> predicate);

    /**
     * Configures a type of failure filtering the failures on which the behavior (specified with the returned
     * {@link UniOnFailure}) is applied.
     * <p>
     * For instance, to only when an {@code IOException} is fired as failure you can use:
     * <code>uni.onFailure(IOException.class).recoverWithItem("hello")</code>
     * <p>
     * The fallback value ({@code hello}) will only be used if the upstream uni fire a failure of type
     * {@code IOException}.
     *
     * @param typeOfFailure the class of exception, must not be {@code null}
     * @return a UniOnFailure configured with the given predicate on which you can specify the on failure action
     */
    UniOnFailure<T> onFailure(Class<? extends Throwable> typeOfFailure);

    /**
     * Produces a {@link Uni} reacting when a no item event is fired by the upstream uni during the specified time
     * period.
     * <p>
     * This {@link Uni} detects if this {@link Uni} does not emit an item before the configured timeout.
     * <p>
     * Examples:
     * <code>
     * uni.ifNoItem().after(Duration.ofMillis(1000).fail() // Propagate a TimeOutException
     * uni.ifNoItem().after(Duration.ofMillis(1000).recoverWithValue("fallback") // Inject a fallback item on timeout
     * uni.ifNoItem().after(Duration.ofMillis(1000).on(myExecutor)... // Configure the executor calling on timeout actions
     * uni.ifNoItem().after(Duration.ofMillis(1000).retry().atMost(5) // Retry five times
     * </code>
     *
     * @return the on timeout group
     */
    UniIfNoItem<T> ifNoItem();

    /**
     * Produces a new {@link Uni} invoking the {@link UniSubscriber#onItem(Object)} and
     * {@link UniSubscriber#onFailure(Throwable)} on the supplied {@link Executor}.
     * <p>
     * Instead of receiving the {@code item} event on the thread firing the event, this method influences the
     * threading context to switch to a thread from the given executor.
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link Uni}
     */
    Uni<T> emitOn(Executor executor);

    /**
     * When a subscriber subscribes to this {@link Uni}, executes the subscription to the upstream {@link Uni} on a thread
     * from the given executor. As a result, the {@link UniSubscriber#onSubscribe(UniSubscription)} method will be called
     * on this thread (except mentioned otherwise)
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link Uni}
     * @deprecated Use {@link #runSubscriptionOn(Executor)} instead
     */
    @Deprecated
    default Uni<T> subscribeOn(Executor executor) {
        return runSubscriptionOn(executor);
    }

    /**
     * When a subscriber subscribes to this {@link Uni}, executes the subscription to the upstream {@link Uni} on a thread
     * from the given executor. As a result, the {@link UniSubscriber#onSubscribe(UniSubscription)} method will be called
     * on this thread (except mentioned otherwise)
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link Uni}
     */
    Uni<T> runSubscriptionOn(Executor executor);

    /**
     * Caches the events (item or failure) of this {@link Uni} and replays it for all further {@link UniSubscriber}.
     *
     * @return the new {@link Uni}. Unlike regular {@link Uni}, re-subscribing to this {@link Uni} does not re-compute
     *         the outcome but replayed the cached events.
     */
    Uni<T> cache();

    /**
     * Transforms the item (potentially null) emitted by this {@link Uni} by applying a (synchronous) function to it.
     * This method is equivalent to {@code uni.onItem().apply(x -> ...)}
     * For asynchronous composition, look at flatMap.
     *
     * @param mapper the mapper function, must not be {@code null}
     * @param <O> the output type
     * @return a new {@link Uni} computing an item of type {@code <O>}.
     */
    default <O> Uni<O> map(Function<? super T, ? extends O> mapper) {
        return onItem().transform(nonNull(mapper, "mapper"));
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when the {@code item} event is fired. Note that the
     * item can be {@code null}.
     * <p>
     * If the callback throws an exception, this exception is propagated to the downstream as failure.
     * <p>
     * This method is a shortcut on {@link UniOnItem#invoke(Consumer)}
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    default Uni<T> invoke(Consumer<? super T> callback) {
        return onItem().invoke(nonNull(callback, "callback"));
    }

    /**
     * Produces a new {@link Uni} invoking the given @{code action} when the {@code item} event is received. Note that
     * the received item can be {@code null}.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its item, this item is discarded, and the original {@code item} is forwarded downstream. If the produced
     * {@code Uni} fails, the failure is propagated downstream. If the callback throws an exception, this exception
     * is propagated downstream as failure.
     * <p>
     * This method is a shortcut on {@link UniOnItem#invokeUni(Function)}
     *
     * @param action the function taking the item and returning a {@link Uni}, must not be {@code null}, must not return
     *        {@code null}
     * @return the new {@link Uni}
     * @deprecated Use {@link #invoke(Function)}
     */
    @Deprecated
    default Uni<T> invokeUni(Function<? super T, Uni<?>> action) {
        return onItem().invokeUni(nonNull(action, "action"));
    }

    /**
     * Produces a new {@link Uni} invoking the given @{code action} when the {@code item} event is received. Note that
     * the received item can be {@code null}.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * its item, this item is discarded, and the original {@code item} is forwarded downstream. If the produced
     * {@code Uni} fails, the failure is propagated downstream. If the callback throws an exception, this exception
     * is propagated downstream as failure.
     * <p>
     * This method is a shortcut on {@link UniOnItem#invokeUni(Function)}
     *
     * @param action the function taking the item and returning a {@link Uni}, must not be {@code null}, must not return
     *        {@code null}
     * @return the new {@link Uni}
     */
    default Uni<T> invoke(Function<? super T, Uni<?>> action) {
        return onItem().invoke(nonNull(action, "action"));
    }

    /**
     * Transforms the received item asynchronously, forwarding the events emitted by another {@link Uni} produced by
     * the given {@code mapper}.
     * <p>
     * The mapper is called with the item event of the current {@link Uni} and produces an {@link Uni}, possibly
     * using another type of item ({@code R}). The events fired by produced {@link Uni} are forwarded to the
     * {@link Uni} returned by this method.
     * <p>
     * This operation is generally named {@code flatMap}.
     * This method is a shortcut on {@link UniOnItem#transformToUni(Function)} onItem().transformToUni(mapper)}.
     *
     * @param mapper the function called with the item of the this {@link Uni} and producing the {@link Uni},
     *        must not be {@code null}, must not return {@code null}.
     * @param <O> the type of item
     * @return a new {@link Uni} that would fire events from the uni produced by the mapper function, possibly
     *         in an asynchronous manner.
     */
    default <O> Uni<O> flatMap(Function<? super T, Uni<? extends O>> mapper) {
        return onItem().transformToUni(nonNull(mapper, "mapper"));
    }

    /**
     * One the observed {@code Uni} emits an item, execute the given {@code mapper}. This mapper produces another
     * {@code Uni}. The downstream receives the events emitted by this produced {@code Uni}.
     *
     * This operation allows <em>chaining</em> asynchronous operations: when the upstream completes with an item, run
     * the mapper and emits the item (or failure) sent by the produced {@code Uni}:
     *
     * <pre>
     * Uni&lt;Session&gt; uni = getSomeSession();
     * return uni.chain(session -&gt; session.persist(fruit))
     *         .chain(session -&gt; session.flush())
     *         .map(x -&gt; Response.ok(fruit).status(201).build());
     * </pre>
     *
     * The mapper is called with the item event of the current {@link Uni} and produces an {@link Uni}, possibly
     * using another type of item ({@code R}). The events fired by produced {@link Uni} are forwarded to the
     * {@link Uni} returned by this method.
     * <p>
     * This operation is generally named {@code flatMap}.
     * This method is a shortcut for {@link UniOnItem#transformToUni(Function) onItem().transformToUni(mapper)}.
     *
     * @param mapper the function called with the item of the this {@link Uni} and producing the {@link Uni},
     *        must not be {@code null}, must not return {@code null}.
     * @param <O> the type of item
     * @return a new {@link Uni} that would fire events from the uni produced by the mapper function, possibly
     *         in an asynchronous manner.
     * @see #then(Supplier)
     */
    default <O> Uni<O> chain(Function<? super T, Uni<? extends O>> mapper) {
        return onItem().transformToUni(nonNull(mapper, "mapper"));
    }

    /**
     * One the observed {@code Uni} emits an item, execute the given {@code supplier}. Unlike {@link #chain(Function)},
     * the received item is not required to run the {@code supplier}, and so omitted. The supplier produces another
     * {@code Uni}. The downstream receives the events emitted by this produced {@code Uni}.
     *
     * This operation allows <em>chaining</em> asynchronous operation when you don't need the previous result: when the
     * upstream completes with an item, run the supplier and emits the item (or failure) sent by the produced
     * {@code Uni}:
     *
     * <pre>
     * {@code
     * String id = ...;
     * Session session = getSomeSession();
     * session.find(Fruit.class, id)
     *        .chain(fruit -> session.remove(fruit)
     *        .then(() -> session.flush());
     * }
     * </pre>
     *
     * This method is a shortcut for {@link UniOnItem#transformToUni(Function)
     * onItem().transformToUni(ignored -> supplier.get())}.
     *
     * @param supplier the function called when the item of the this {@link Uni} is emitted and producing the {@link Uni},
     *        must not be {@code null}, must not return {@code null}.
     * @param <O> the type of item
     * @return a new {@link Uni} that would fire events from the uni produced by the mapper function, possibly
     *         in an asynchronous manner.
     * @see #chain(Function)
     */
    default <O> Uni<O> then(Supplier<Uni<? extends O>> supplier) {
        Supplier<Uni<? extends O>> actual = nonNull(supplier, "supplier");
        return onItem().transformToUni(ignored -> actual.get());
    }

    /**
     * Execute an action after an item or a failure has been emitted.
     * This is equivalent to a {@code finally} block in Java.
     *
     * <pre>
     * {@code
     * String id = ...;
     * Session session = getSomeSession();
     * session.find(Fruit.class, id)
     *        .chain(fruit -> session.remove(fruit)
     *        .then(() -> session.flush())
     *        .eventually(() -> session.close());
     * }
     * </pre>
     * <p>
     * This method is a shortcut for {@link UniOnItemOrFailure#invoke(BiConsumer)}:
     * {@code onItemOrFailure().invoke((item, err) -> action.run())}
     *
     * @param action an action to perform, must not be {@code null}.
     * @return a new {@link Uni} that emits events once the action has completed.
     * @see #onItemOrFailure()
     */
    default Uni<T> eventually(Runnable action) {
        return onItemOrFailure().invoke((item, err) -> nonNull(action, "action").run());
    }

    /**
     * When this {@link Uni} emits an item or a failure, invoke a {@link Uni} supplier then invoke the supplied {@link Uni}.
     * When the supplied {@link Uni} emits an item then it is ignored, and when it emits a failure it is reported.
     * <p>
     * This is equivalent to a {@code finally} block in Java.
     *
     * <pre>
     * {@code
     * String id = ...;
     * Session session = getSomeSession();
     * session.find(Fruit.class, id)
     *        .chain(fruit -> session.remove(fruit)
     *        .eventually(() -> session.close());
     * }
     * </pre>
     * <p>
     * This method is a shortcut for {@link UniOnItemOrFailure#invokeUni(BiFunction)}:
     * {@code onItemOrFailure().invokeUni((item, err) -> supplier.get())}
     *
     * @param supplier a {@link Uni} supplier, cannot be {@code null} and cannot return {@code null}.
     * @param <O> the type of the item
     * @return a new {@link Uni} that emits events once the supplied {@link Uni} emits an item or a failure.
     * @see #onItemOrFailure()
     */
    default <O> Uni<T> eventually(Supplier<Uni<? extends O>> supplier) {
        Supplier<Uni<? extends O>> actual = nonNull(supplier, "supplier");
        return onItemOrFailure().invokeUni((item, err) -> actual.get());
    }

    /**
     * Converts an {@link Uni} to other types such as {@link CompletionStage}
     *
     * <p>
     * Examples:
     * </p>
     *
     * <pre>
     * {@code
     * uni.convert().toCompletionStage(); // Convert to CompletionStage using convenience method
     * uni.convert().with(BuiltinConverters.toCompletionStage()); // Convert to CompletionStage using BuiltInConverters
     * uni.convert().with(uni -> x); // Convert with a custom lambda converter
     * }
     * </pre>
     *
     * @return the object to convert an {@link Uni} instance
     * @see UniConvert
     */
    UniConvert<T> convert();

    /**
     * Creates an instance of {@link Multi} from this {@link Uni}.
     * <p>
     * When a subscriber subscribes to the returned {@link Multi} and <strong>request</strong> an item, it subscribes
     * to this {@link Uni} and the events from this {@link Uni} are propagated to the {@link Multi}:
     * <ul>
     * <li>if this {@link Uni} emits a non-{@code null} item - this item is propagated to the {@link Multi}
     * and followed with the completion event</li>
     * <li>if this {@link Uni} emits a {@code null} item - the {@link Multi} fires the completion event</li>
     * <li>if this {@link Uni} emits a failure, this failure event is propagated by the {@link Multi}</li>
     * </ul>
     * <p>
     * It's important to note that the subscription to this {@link Uni} happens when the subscriber to the produced
     * {@link Multi} <strong>requests</strong> items, and not at subscription time.
     *
     * @return the produced {@link Multi}, never {@code null}
     */
    Multi<T> toMulti();

    /**
     * Allows adding behavior when various type of events are emitted by the current {@link Uni} (item, failure) or
     * by the subscriber (cancellation, subscription)
     *
     * @return the object to configure the action to execute when events happen
     */
    UniOnEvent<T> on();

    /**
     * Allows configuring repeating behavior.
     * Repeating allow transforming a {@link Uni} into a {@link Multi} either a specific amount of times or indefinitely.
     * Each time, a new subscription is attempted on the {@link Uni}.
     * Cancelling the subscription stops the repeating behavior.
     *
     * @return the object to configure the repeating behavior.
     */
    UniRepeat<T> repeat();

    /**
     * Configures actions to be performed on termination, that is, on item, on failure, or when the subscriber cancels
     * the subscription.
     * 
     * @return the object to configure the termination actions.
     */
    UniOnTerminate<T> onTermination();

    /**
     * Configures actions to be performed when the subscriber cancels the subscription.
     * 
     * @return the object to configure the cancellation actions.
     */
    UniOnCancel<T> onCancellation();
}
