package io.smallrye.reactive;

import io.smallrye.reactive.groups.*;
import io.smallrye.reactive.subscription.UniSubscriber;
import io.smallrye.reactive.subscription.UniSubscription;
import io.smallrye.reactive.tuples.Pair;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

/**
 * A {@link Uni} represent a lazy asynchronous action. It follows a subscription pattern, meaning the the action
 * is only triggered once a {@link io.smallrye.reactive.subscription.UniSubscriber} subscribes to the {@link Uni}.
 * <p>
 * A {@link Uni} can have two outcomes:
 * <ol>
 * <li>A {@code result} event, forwarding the result of the action, potentially {@code null}</li>
 * <li>A {@code failure} event, forwarding the exception</li>
 * </ol>
 * <p>
 * To trigger the computation, a {@link UniSubscriber} must subscribe to the Uni. It will be notified of outcome
 * once they are {@code result} or {@code failure} events are fired by the observed Uni. A subscriber receives
 * (asynchronously) a {@link UniSubscription} and can cancel the demand at any time. Note that cancelling after
 * having received the outcome is a no-op.
 * <p>
 *
 * @param <T> the type of result produced by the {@link Uni}
 */
public interface Uni<T> {

    /**
     * Creates a new {@link Uni} from various sources such as {@link CompletionStage},
     * {@link io.smallrye.reactive.subscription.UniEmitter}, direct values, {@link Exception}...
     *
     * <p>Examples:</p>
     * <pre>{@code
     * Uni.from().value(1); // Emit 1 at subscription time
     * Uni.from().value(() -> x); // Emit x at subscription time, the supplier is invoked for each subscription
     * Uni.from().completionState(cs); // Emit the result from this completion stage
     * Uni.from().completionState(() -> cs); // Emit the result from this completion stage, the stage is not created before subscription
     * Uni.from().failure(exception); // Emit the failure at subscription time
     * Uni.from().deferred(() -> Uni.from().value(x)); // Defer the uni creation until subscription. Each subscription can produce a different uni
     * Uni.from().nullValue(); // Emit null at subscription time
     * Uni.from().nothing(); // Create a Uni not emitting any signal
     * Uni.from().publisher(publisher); // Create a Uni from a Reactive Streams Publisher
     * }</pre>
     *
     * @return the factory used to create {@link Uni} instances.
     * @see UniCreate
     */
    static UniCreate createFrom() {
        return UniCreate.INSTANCE;
    }

    /**
     * Requests the {@link Uni} to start resolving the result and allows configuring how the signals are propagated
     * (using a {@link UniSubscriber}, callbacks, or a {@link CompletionStage}. Unlike {@link #await()}, this method
     * configures non-blocking retrieval of the result and failure.
     *
     * <p>Examples:</p>
     * <pre>{@code
     *     Uni<String> uni = ...;
     *
     *    Subscription sub = uni.subscribe().with( // The return subscription can be used to cancel the operation
     *              result -> {},           // Callback calls on result
     *              failure -> {}           // Callback calls on failure
     *    );
     *
     *    UniSubscriber<String> myUniSubscriber = ...
     *    uni.subscribe().withSubscriber(myUniSubscriber); // Subscribes to the Uni with the passed subscriber
     *
     *    CompletableFuture future = uni.subscribe().asCompletableFuture(); // Get a CompletionStage receiving the result or failure
     *    // Cancelling the returned future cancels the subscription.
     * }</pre>
     *
     * @return the object to configure the subscription.
     * @see #await() <code>uni.await() </code>for waiting (blocking the caller thread) until the resolution of the observed Uni.
     */
    UniSubscribe<T> subscribe();

    /**
     * Awaits (blocking the caller thread) until the result or a failure is emitted by the observed {@link Uni}.
     * If the observed uni fails, the failure is thrown. In the case of a checked exception, the exception is wrapped
     * into a {@link java.util.concurrent.CompletionException}.
     *
     * <p>Examples:</p>
     * <pre>{@code
     * Uni<T> uni = ...;
     * T res = uni.await().indefinitely(); // Await indefinitely until it get the result.
     * T res = uni.await().atMost(Duration.ofMillis(1000)); // Awaits at most 1s. After that, a TimeoutException is thrown
     * Optional<T> res = uni.await().asOptional().indefinitely(); // Retrieves the result as an Optional, empty if the result is null
     * }</pre>
     *
     * @return the object to configure the retrieval.
     */
    UniAwait<T> await();


    /**
     * Configures the action to execute when the observed {@link Uni} emits the result (potentially {@code null}).
     *
     * <p>Examples:</p>
     * <pre>{@code
     * Uni<T> uni = ...;
     * T res = uni.onResult().mapToResult(x -> ...); // Map to another result
     * T res = uni.onResult().mapToUni(x -> ...); // Map to another Uni (flatMap)
     * }</pre>
     *
     * @return the object to configure the action to execute when a result is emitted
     * @see #onNullResult()
     * @see #onNoResult()
     */
    UniOnResult<T> onResult();

    /**
     * Combines a set of {@link Uni unis} into a joined result. This result can be a {@code Tuple} or the result of a
     * combinator function.
     * <p>
     * If one of the combine {@link Uni} fire a failure, the other unis are cancelled, and the resulting
     * {@link Uni} fires the failure. If {@code awaitCompletion()}  is called,
     * it waits for the completion of all the {@link Uni unis} before propagating the failure event. If more than one
     * {@link Uni} failed, a {@link CompositeException} is fired, wrapping the different collected failures.
     * <p>
     * Depending on the number of participants, the produced {@link io.smallrye.reactive.tuples.Tuple} is
     * different from {@link Pair} to {@link io.smallrye.reactive.tuples.Tuple5}. For more participants,
     * use {@link io.smallrye.reactive.groups.UniAndGroup#unis(Uni[])} or
     * {@link io.smallrye.reactive.groups.UniAndGroup#unis(Iterable)}.
     *
     * @return the object to configure the join
     * @see Uni#zip() <code>Uni.zip()</code> for the equivalent static operator
     */
    UniAndGroup<T> and();

    /**
     * Combines the result of this {@link Uni} with the result of {@code other} into a {@link Pair}.
     * If {@code this} or {@code other} fails, the other resolution is cancelled.
     *
     * @param other the other {@link Uni}, must not be {@code null}
     * @param <T2>  the type to pair
     * @return the combination of the 2 results.
     * @see #and() <code>and</code> for more options on the combination of results
     * @see Uni#zip() <code>Uni.zip()</code> for the equivalent static operator
     */
    <T2> Uni<Pair<T, T2>> and(Uni<T2> other);

    /**
     * Composes this {@link Uni} with a set of {@link Uni} passed to
     * {@link io.smallrye.reactive.groups.UniOr#unis(Uni[])} to produce a new {@link Uni} forwarding the first event
     * (result or failure). It behaves like the fastest of these competing unis.
     * <p>
     * The process subscribes to the set of {@link Uni}. When one of the {@link Uni} fires a result or a failure,
     * the event is propagated downstream. Also the other subscriptions are cancelled.
     * <p>
     * Note that the callback from the subscriber are called on the thread used to fire the winning {@link Uni}.
     * Use {@link #handleResultOn(Executor)} to change the thread.
     * <p>
     * If the subscription to the returned {@link Uni} is cancelled, the subscription to the {@link Uni unis} from the
     * {@code iterable} are also cancelled.
     *
     * @return the object to enlist the participants
     * @see #any() <code>Uni.any</code> for a static version of this operator, like <code>Uni first = Uni.any().of(uni1, uni2);</code>
     */
    UniOr or();

    /**
     * Adds specific behavior when the observed {@link Uni} fires {@code null} as result. While {@code null} is a valid
     * value, it may require specific processing. This group of operators allows implementing this specific behavior.
     *
     * <p>Examples:</p>
     * <pre><code>
     *     Uni&lt;T&gt; upstream = ...;
     *     Uni&lt;T&gt; uni = ...;
     *     uni = upstream.onNullResult().continueWith(anotherValue) // use the fallback value if upstream emits null
     *     uni = upstream.onNullResult().fail() // propagate a NullPointerException if upstream emits null
     *     uni = upstream.onNullResult().failWith(exception) // propagate the given exception if upstream emits null
     *     uni = upstream.onNullResult().switchTo(another) // switch to another uni if upstream emits null
     * </code></pre>
     *
     * @return the object to configure the behavior when receiving {@code null}
     */
    UniOnNullResult<T> onNullResult();

    /**
     * Produces a new {@link Uni} invoking the given callback when this {@link Uni} fires a result or a failure.
     *
     * @param callback the callback called with the result (potentially null) or the failure; must not be {@code null}
     * @return the new {@link Uni}
     */
    Uni<T> onTerminate(BiConsumer<? super T, Throwable> callback);

    /**
     * Produces a new {@link Uni} invoking the given callback when a subscription to the upstream {@link Uni} is
     * cancelled.
     *
     * @param callback the callback called on cancellation.
     * @return the new {@link Uni}
     */
    Uni<T> onCancellation(Runnable callback);

    /**
     * Produces a new {@link Uni} invoking the given consumer when a subscriber subscribes to this {@link Uni}.
     *
     * @param callback the consumer, must not be {@code null}
     * @return the new {@link Uni}
     */
    Uni<T> onSubscription(Consumer<? super UniSubscription> callback);


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
     * <code>uni.onFailure(IOException.class).recoverWithResult("hello")</code>
     * <p>
     * The fallback value ({@code hello}) will only be used if the upstream uni fire a failure of type
     * {@code IOException}.*
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
     * <code>uni.onFailure(IOException.class).recoverWithResult("hello")</code>
     * <p>
     * The fallback value ({@code hello}) will only be used if the upstream uni fire a failure of type
     * {@code IOException}.*
     *
     * @param typeOfFailure the class of exception, must not be {@code null}
     * @return a UniOnFailure configured with the given predicate on which you can specify the on failure action
     */
    UniOnFailure<T> onFailure(Class<? extends Throwable> typeOfFailure);

    /**
     * Produces a {@link Uni} reacting when a no result event is fired by the upstream uni during the specified time
     * period.
     * <p>
     * This {@link Uni} detects if this  {@link Uni} does not emit a result before the configured timeout.
     * <p>
     * Examples:
     * <code>
     * uni.onNoResult().after(Duration.ofMillis(1000).fail() // Propagate a TimeOutException
     * uni.onNoResult().after(Duration.ofMillis(1000).recoverWithValue("fallback") // Inject a fallback result on timeout
     * uni.onNoResult().after(Duration.ofMillis(1000).on(myExecutor)... // Configure the executor calling on timeout actions
     * uni.onNoResult().after(Duration.ofMillis(1000).retry().atMost(5) // Retry five times
     * </code>
     *
     * @return the on timeout group
     */
    UniOnTimeout<T> onNoResult();

    /**
     * Produces a new {@link Uni} invoking the {@link UniSubscriber#onResult(Object)} on the supplied {@link Executor}.
     * <p>
     * Instead of receiving the {@code result} event on the thread firing the event, this method  influences the
     * threading context to switch to a thread from the given executor.
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link Uni}
     */
    Uni<T> handleResultOn(Executor executor);

    /**
     * Produces a new {@link Uni} invoking the {@link UniSubscriber#onFailure(Throwable)} on the supplied {@link Executor}.
     * <p>
     * Instead of receiving the {@code failure} event on the thread firing the event, this method influences the
     * threading context to switch to a thread from the given executor.
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link Uni}
     */
    Uni<T> handleFailureOn(Executor executor);

    /**
     * When a subscriber subscribes to this {@link Uni}, execute the subscription to the upstream {@link Uni} on a thread
     * from the given executor. As a result the {@link UniSubscriber#onSubscribe(UniSubscription)} method will be called
     * on this thread (except mentioned otherwise)
     *
     * @param executor the executor to use, must not be {@code null}
     * @return a new {@link Uni}
     */
    Uni<T> callSubscribeOn(Executor executor);

    /**
     * Caches the events (result or failure) of this {@link Uni} and replays it for all further {@link UniSubscriber}.
     *
     * @return the new {@link Uni}. Unlike regular {@link Uni}, re-subscribing to this {@link Uni} does not re-compute
     * the outcome but replayed the cached events.
     */
    Uni<T> cache();

    /**
     * Creates a {@link Uni} forwarding the first event (result or failure). It behaves like the fastest
     * of these competing unis. If the passed iterable is empty, the resulting {@link Uni} gets a {@code null} result
     * just after subscription.
     * <p>
     * This method subscribes to the set of {@link Uni}. When one of the {@link Uni} fires a result or a failure
     * a failure, the event is propagated downstream. Also the other subscriptions are cancelled.
     * <p>
     * Note that the callback from the subscriber are called on the thread used to fire the event of the selected
     * {@link Uni}. Use {@link Uni#handleResultOn(Executor)} to change that thread.
     * <p>
     * If the subscription to the returned {@link Uni} is cancelled, the subscription to the {@link Uni unis}
     * contained in the {@code iterable} are also cancelled.
     *
     * @return the object to enlist the candidates
     * @see Uni#or <code>Uni.or()</code> for the equivalent operator on Uni instances
     */
    static UniAny any() {
        return UniAny.INSTANCE;
    }

    /**
     * Combines a set of {@link Uni unis} into a joined result. This result can be a {@code Tuple} or the result of a
     * combinator function.
     * <p>
     * If one of the combine {@link Uni} fire a failure, the other unis are cancelled, and the resulting
     * {@link Uni} fires the failure. If {@code awaitCompletion()}  is called,
     * it waits for the completion of all the {@link Uni unis} before propagating the failure event. If more than one
     * {@link Uni} failed, a {@link CompositeException} is fired, wrapping the different collected failures.
     * <p>
     * Depending on the number of participants, the produced {@link io.smallrye.reactive.tuples.Tuple} is
     * different from {@link Pair} to {@link io.smallrye.reactive.tuples.Tuple5}. For more participants,
     * use {@link io.smallrye.reactive.groups.UniZip#unis(Uni[])} or
     * {@link io.smallrye.reactive.groups.UniZip#unis(Iterable)}.
     *
     * @return the object to configure the join
     * @see Uni#and <code>Uni.and()</code> for the equivalent operator on Uni instances
     */
    static UniZip zip() {
        return UniZip.INSTANCE;
    }

    /**
     * Transforms the result (potentially null) emitted by this {@link Uni} by applying a (synchronous) function to it.
     * This method is equivalent to {@code uni.onResult().mapToResult(x -> ...)}
     * For asynchronous composition, look at flatMap.
     *
     * @param mapper the mapper function, must not be {@code null}
     * @param <O>    the output type
     * @return a new {@link Uni} computing a result of type {@code <O>}.
     */
    default <O> Uni<O> map(Function<T, O> mapper) {
        return onResult().mapToResult(nonNull(mapper, "mapper"));
    }

    //TODO
    UniAdapt<T> adapt();

    /**
     * Creates an instance of {@link Multi} from this {@link Uni}.
     * <p>
     * When a subscriber subscribes to the returned {@link Multi} and <strong>request</strong> a result, it subscribes
     * to this {@link Uni} and the events from this {@link Uni} are propagated to the {@link Multi}:
     * <ul>
     * <li>if this {@link Uni} emits a non-{@code null} result - this result is propagated to the {@link Multi}
     * and followed with the completion event</li>
     * <li>if this {@link Uni} emits a {@code null} result - the {@link Multi} fires the completion event</li>
     * <li>if this {@link Uni} emits a failure, this failure event is propagated by the {@link Multi}</li>
     * </ul>
     * <p>
     * It's important to note that the subscription to this {@link Uni} happens when the subscriber to the produced
     * {@link Multi} requests results, and not at subscription time.
     *
     * @return the produced {@link Multi}, never {@code null}
     */
    Multi<T> toMulti();
}
