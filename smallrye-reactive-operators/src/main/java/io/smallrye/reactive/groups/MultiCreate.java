package io.smallrye.reactive.groups;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.*;
import io.smallrye.reactive.subscription.BackPressureStrategy;
import io.smallrye.reactive.subscription.MultiEmitter;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.smallrye.reactive.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;


/**
 * Group methods allowing to create {@link Multi} instances from various sources.
 */
public class MultiCreate {

    public static final MultiCreate INSTANCE = new MultiCreate();

    private MultiCreate() {
        // avoid direct instantiation.
    }

    /**
     * Creates a {@link Multi} from the given {@link CompletionStage} or {@link CompletableFuture}.
     * The produced {@code Multi} emits the result of the passed  {@link CompletionStage} and then fire the completion
     * event. If the {@link CompletionStage} never completes (or failed), the produced {@link Multi} would not emit
     * any {@code result} or {@code failure} events.
     * <p>
     * Cancelling the subscription on the produced {@link Multi} cancels the passed {@link CompletionStage}
     * (calling {@link CompletableFuture#cancel(boolean)} on the future retrieved using
     * {@link CompletionStage#toCompletableFuture()}.
     * <p>
     * If the stage has already been completed (or failed), the produced {@link Multi} sends the result or failure
     * immediately after subscription. If it's not the case, the subscriber's callbacks are called on the thread used
     * by the passed {@link CompletionStage}.
     * If the completion stage redeems {@code null}, it fires the completion event without any result.
     *
     * @param stage the stage, must not be {@code null}
     * @param <T>   the type of result
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> completionStage(CompletionStage<? extends T> stage) {
        CompletionStage<? extends T> actual = nonNull(stage, "stage");
        return completionStage(() -> actual);
    }

    /**
     * Creates a {@link Multi} from the given {@link CompletionStage} or {@link CompletableFuture}. The future is
     * created by invoking the passed {@link Supplier} <strong>lazily</strong> at subscription time.
     * <p>
     * The produced {@code Multi} emits the result of the passed  {@link CompletionStage} followed by the completion
     * event. If the {@link CompletionStage} never completes (or failed), the produced {@link Multi} would not emit
     * a result or a failure.
     * <p>
     * Cancelling the subscription on the produced {@link Multi} cancels the passed {@link CompletionStage}
     * (calling {@link CompletableFuture#cancel(boolean)} on the future retrieved using
     * {@link CompletionStage#toCompletableFuture()}.
     * <p>
     * If the produced stage has already been completed (or failed), the produced {@link Multi} sends the result or
     * failure immediately after subscription. In the case of result, the event is followed by the completion event. If
     * the produced stage is not yet completed,  the subscriber's callbacks are called on the thread used
     * by the passed {@link CompletionStage}.
     * <p>
     * If the produced completion stage redeems {@code null}, it fires the completion event without any result.
     * <p>
     * If the supplier throws an exception, a failure event with the exception is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not produce {@code null}
     * @param <T>      the type of result
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> completionStage(Supplier<? extends CompletionStage<? extends T>> supplier) {
        return new MultiCreateFromCompletionStage<>(nonNull(supplier, "supplier"));
    }

    /**
     * Creates a {@link Multi} from the passed {@link Publisher}.
     * <p>
     * When a subscriber subscribes to the produced {@link Multi}, it subscribes to the {@link Publisher} and delegate
     * the requests. Note that each Multi's subscriber would produce a new subscription.
     * <p>
     * If the Multi's observer cancels its subscription, the subscription to the {@link Publisher} is also cancelled.
     *
     * @param publisher the publisher, must not be {@code null}
     * @param <T>       the type of result
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> publisher(Publisher<? extends T> publisher) {
        Publisher<? extends T> actual = nonNull(publisher, "publisher");
        return new AbstractMulti<T>() {
            @Override
            protected Flowable<T> flowable() {
                return Flowable.fromPublisher(actual);
            }
        };
    }

    /**
     * Creates a new {@link Multi} that emits a result immediately after being subscribed to with the specified single
     * (potentially {@code null}) value. The value is retrieved <strong>lazily</strong> at subscription time, using
     * the passed {@link Supplier}. Unlike {@link #deferred(Supplier)}, the supplier produces a result and not a
     * {@link Multi}.
     * <p>
     * If the supplier produces {@code null}, the produced {@link Multi} fires the completion event.
     * If the supplier produces a non-{@code null} result, the produced {@link Multi} fires a result event followed with
     * the completion event.
     * If the supplier throws an exception, a failure event with the exception  is fired.
     *
     * @param supplier the result supplier, must not be {@code null}, can produce {@code null}
     * @param <T>      the type of result emitted by the produced Multi
     * @return the new {@link Multi}
     */
    public <T> Multi<T> result(Supplier<? extends T> supplier) {
        Supplier<? extends T> actual = nonNull(supplier, "supplier");
        return emitter(emitter -> {
            T result;
            try {
                result = actual.get();
            } catch (RuntimeException e) {
                // Exception from the supplier, propagate it.
                emitter.failure(e);
                return;
            }
            if (result != null) {
                emitter.result(result);
            }
            emitter.complete();
        });
    }

    /**
     * Creates a new {@link Multi} that emits the results immediately after being subscribed to. The individual result
     * comes from the {@link Stream} supplied by the given {@link Supplier}. This supplier is called at subscription
     * time.
     * <p>
     * If the supplier produces {@code null}, the produced {@link Multi} fires a failure event.
     * If the supplier produces an empty stream, the produced {@link Multi} fires a completion event.
     * For each items from the supplied stream, a result event is fired. When all the results have been emitted,
     * the completion event is fired.
     * If the supplier throws an exception, a failure event with the exception is fired.
     * the stream is consumed sequentially.
     *
     * @param supplier the result supplier, must not be {@code null}, must not produce {@code null}
     * @param <T>      the type of result emitted by the produced Multi
     * @return the new {@link Multi}
     */
    public <T> Multi<T> results(Supplier<? extends Stream<? extends T>> supplier) {
        Supplier<? extends Stream<? extends T>> actual = nonNull(supplier, "supplier");
        return emitter(emitter -> {
            Stream<? extends T> stream;
            try {
                stream = actual.get();
            } catch (RuntimeException e) {
                // Exception from the supplier, propagate it.
                emitter.failure(e);
                return;
            }
            if (stream == null) {
                emitter.failure(new NullPointerException(SUPPLIER_PRODUCED_NULL));
                return;
            }
            AtomicBoolean failed = new AtomicBoolean();
            stream
                    .sequential()
                    .forEach(result -> {
                        if (result == null) {
                            failed.set(true);
                            emitter.failure(new IllegalArgumentException("The iterable contained a `null` value"));
                            return;
                        }
                        emitter.result(result);
                    });
            if (!failed.get()) {
                emitter.complete();
            }
        });
    }

    /**
     * Creates a new {@link Multi} that emits a result immediately after being subscribed to with the specified single
     * result. If {@code result} is {@code null} the completion event is fired immediately making the resulting
     * {@link Multi} empty. If {@code result} is non-{@code null}, the result event is fired immediately followed with
     * the completion event.
     *
     * @param result the result, can be {@code null} which would create an empty {@link Multi}
     * @param <T>    the type of result emitted by the produced Multi
     * @return the new {@link Multi}
     */
    public <T> Multi<T> result(T result) {
        return result(() -> result);
    }

    /**
     * Creates a new {@link Multi} that emits the results individually after being subscribed to (according to the
     * subscriber's request).
     * <p>
     * If {@code results} is {@code null}, an {@link IllegalArgumentException} is thrown at call time.
     * If one of the result from {@code results} is {@code null}, a failure event is fired (with an
     * {@link IllegalArgumentException}).
     * When all the results have been emitted, the completion event is fired.
     *
     * @param results the results, must not be {@code null}, must not contain {@code null}
     * @param <T>     the type of result emitted by the produced Multi
     * @return the new {@link Multi}
     */
    @SafeVarargs
    public final <T> Multi<T> individualResults(T... results) {
        return results(Arrays.asList(nonNull(results, "results")));
    }

    /**
     * Creates a new {@link Multi} that emits the results individually after being subscribed to (according to the
     * subscriber's request).
     * <p>
     * If {@code results} is {@code null}, an {@link IllegalArgumentException} is thrown at call time.
     * If one of the result from {@code results} is {@code null}, a failure event is fired (with an
     * {@link IllegalArgumentException}).
     * When all the results have been emitted, the completion event is fired.
     *
     * @param results the results, must not be {@code null}, must not contain {@code null}
     * @param <T>     the type of result emitted by the produced Multi
     * @return the new {@link Multi}
     */
    public <T> Multi<T> results(Iterable<T> results) {
        return results(() -> StreamSupport.stream(results.spliterator(), false));
    }

    /**
     * Creates a new {@link Multi} that emits the results fromt he passed {@link Stream} individually after being
     * subscribed to (according to the subscriber's request).
     * <p>
     * If {@code results} is {@code null}, an {@link IllegalArgumentException} is thrown at call time.
     * If one of the result from the stream is {@code null}, a failure event is fired (with an
     * {@link IllegalArgumentException}).
     * When all the results have been emitted, the completion event is fired.
     * The stream is consumed sequentially.
     *
     * @param results the results, must not be {@code null}, must not contain {@code null}
     * @param <T>     the type of result emitted by the produced Multi
     * @return the new {@link Multi}
     */
    public <T> Multi<T> results(Stream<T> results) {
        Stream<T> stream = nonNull(results, "results");
        return results(() -> stream);
    }

    /**
     * Creates a new {@link Multi} that emits a result immediately after being subscribed to with the value contained
     * in the given optional if {@link Optional#isPresent()} or empty otherwise.
     *
     * @param optional the optional, must not be {@code null}, an empty optional produces an empty {@link Multi}.
     * @param <T>      the type of the produced result
     * @return the new {@link Multi}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public <T> Multi<T> optional(Optional<T> optional) {
        Optional<T> actual = nonNull(optional, "optional");
        return result(() -> actual.orElse(null));
    }

    /**
     * Creates a new {@link Multi} that emits a result immediately after being subscribed to with the value contained
     * in the optional supplied by {@code supplier}.
     * <p>
     * If the optional is empty, an empty {@link Multi} is produced. Otherwise the contained value is emitted as result,
     * followed with the completion event.
     * Unlike {@link #optional(Optional)}, the passed {@link Supplier} is called lazily at subscription time.
     * <p>
     * If the supplier throws an exception, a failure event with the exception  is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not return {@code null}
     * @param <T>      the type of the produced result
     * @return the new {@link Multi}
     */
    public <T> Multi<T> optional(Supplier<Optional<T>> supplier) {
        Supplier<Optional<T>> actual = nonNull(supplier, "supplier");
        return result(() -> actual.get().orElse(null));
    }

    /**
     * Like {@link #emitter(Consumer, BackPressureStrategy)} with the {@link BackPressureStrategy#BUFFER} strategy.
     *
     * @param consumer the consumer receiving the emitter, must not be {@code null}
     * @param <T>      the type of result emitted by the produced Multi
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> emitter(Consumer<MultiEmitter<? super T>> consumer) {
        return emitter(consumer, BackPressureStrategy.BUFFER);
    }

    /**
     * Creates a {@link Multi} deferring the logic to the given consumer. The consumer can be used with callback-based
     * APIs to fire results (non-{@code null}), failure or completion events.
     * <p>
     * Emitting {@code null} value is not supported. Emitting values after having fired a failure or the completion
     * event is a no-op. So subsequent result events are dropped.
     * <p>
     * Using this method, you can produce a {@link Multi} based on listener or callbacks APIs. You register the listener
     * in the consumer and emits the results / failure / completion events when the listener is invoked. Don't forget
     * to unregister the listener on cancellation.
     * <p>
     * If the consume throws an exception, a failure event with the exception is fired.
     *
     * @param consumer callback receiving the {@link MultiEmitter} and events downstream. The callback is
     *                 called for each subscriber (at subscription time). Must not be {@code null}
     * @param strategy the back pressure strategy to apply when the downstream subscriber cannot keep up with the
     *                 results emitted by the emitter.
     * @param <T>      the type of results
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> emitter(Consumer<MultiEmitter<? super T>> consumer, BackPressureStrategy strategy) {
        Consumer<MultiEmitter<? super T>> actual = nonNull(consumer, "consumer");
        return new MultiCreateWithEmitter<>(actual, strategy);
    }

    /**
     * Creates a {@link Multi} that {@link Supplier#get supplies} an {@link Multi} to subscribe to for each
     * {@link Subscriber}. The supplier is called at subscription time.
     * <p>
     * In practice, it defers the {@link Multi} creation at subscription time and allows each subscriber to get different
     * {@link Multi}. So, it does not create the {@link Multi} until an {@link Subscriber subscriber} subscribes, and
     * creates a fresh {@link Multi} for each subscriber.
     * <p>
     * Unlike {@link #result(Supplier)}, the supplier produces an {@link Multi} (and not a result).
     * <p>
     * If the supplier throws an exception, a failure event with the exception  is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not produce {@code null}
     * @param <T>      the type of result
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> deferred(Supplier<? extends Multi<? extends T>> supplier) {
        Supplier<? extends Multi<? extends T>> actual = nonNull(supplier, "supplier");
        return new MultiCreateFromDeferredSupplier<>(actual);
    }

    /**
     * Creates a {@link Multi} that emits a {@code failure} event immediately after being subscribed to.
     *
     * @param failure the failure to be fired, must not be {@code null}
     * @param <T>     the virtual type of result used by the {@link Multi}, must be explicitly set as in
     *                {@code Multi.<String>failed(exception);}
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> failure(Throwable failure) {
        Throwable exception = nonNull(failure, "failure");
        return failure(() -> exception);
    }

    /**
     * Creates a {@link Multi} that emits a {@code failure} event produced using the passed supplier immediately after
     * being subscribed to. The supplier is called at subscription time, and produces an instance of {@link Throwable}.
     * If the supplier throws an exception, a {@code failure} event is fired with this exception.
     * If the supplier produces {@code null}, a {@code failure} event is fired with a {@link NullPointerException}.
     *
     * @param supplier the supplier producing the failure, must not be {@code null}, must not produce {@code null}
     * @param <T>      the virtual type of result used by the {@link Multi}, must be explicitly set as in
     *                 {@code Multi.<String>failed(exception);}
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> failure(Supplier<Throwable> supplier) {
        Supplier<Throwable> actual = nonNull(supplier, "supplier");
        return new AbstractMulti<T>() {
            @Override
            protected Flowable<T> flowable() {
                return Flowable.error(actual::get);
            }
        };
    }

    /**
     * Creates a {@link Multi} that will never fire any events.
     *
     * @param <T> the virtual type of result
     * @return a never emitting {@link Multi}
     */
    @SuppressWarnings("unchecked")
    public <T> Multi<T> nothing() {
        return (Multi<T>) MultiNever.INSTANCE;
    }

    /**
     * Creates a {@link Multi} that fires the completion event without having emitted any results.
     * An empty {@link Multi} does not fires a failure event either.
     *
     * @param <T> the virtual type of result
     * @return an empty {@link Multi}
     */
    @SuppressWarnings("unchecked")
    public <T> Multi<T> empty() {
        return (Multi<T>) MultiEmpty.INSTANCE;
    }

    /**
     * Creates a {@link Multi} that emits long results (ticks) starting with 0 and incrementing at
     * specified time intervals.
     * <p>
     * Be aware that if the subscriber does not request enough result in time, a back pressure failure is fired.
     * The produced {@link Multi} never completes until cancellation by the subscriber.
     * <p>
     * The callbacks are invoked on the executor passed in {@link MultiTimePeriod#onExecutor(ScheduledExecutorService)}.
     *
     * @return the object to configure the time period (initial delay, executor, interval)
     */
    public MultiTimePeriod ticks() {
        return new MultiTimePeriod();
    }

    /**
     * Creates a {@link Multi} emitting the sequence of integer from {@code startInclusive} to {@code endExclusive}.
     * Once all the integers have been emitted, the completion event is fired.
     *
     * @param startInclusive the start integer (inclusive)
     * @param endExclusive   the end integer (exclusive)
     * @return the {@link Multi} emitting the results with the integers
     */
    public Multi<Integer> range(int startInclusive, int endExclusive) {
        if (endExclusive <= startInclusive) {
            throw new IllegalArgumentException("end must be greater than start");
        }
        return Multi.createFrom().results(() -> IntStream.range(startInclusive, endExclusive).iterator());
    }


//    public <T, X> Multi<T> converterOf(X instance) {
//        return MultiAdaptFrom.adaptFrom(instance);
//    }
}
