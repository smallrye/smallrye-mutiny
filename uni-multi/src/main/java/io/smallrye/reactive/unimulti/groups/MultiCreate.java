package io.smallrye.reactive.unimulti.groups;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.operators.*;
import io.smallrye.reactive.unimulti.subscription.BackPressureStrategy;
import io.smallrye.reactive.unimulti.subscription.MultiEmitter;

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
     * The produced {@code Multi} emits the item of the passed {@link CompletionStage} and then fire the completion
     * event. If the {@link CompletionStage} never completes (or failed), the produced {@link Multi} would not emit
     * any {@code item} or {@code failure} events.
     * <p>
     * Cancelling the subscription on the produced {@link Multi} cancels the passed {@link CompletionStage}
     * (calling {@link CompletableFuture#cancel(boolean)} on the future retrieved using
     * {@link CompletionStage#toCompletableFuture()}.
     * <p>
     * If the stage has already been completed (or failed), the produced {@link Multi} sends the item or failure
     * immediately after subscription. If it's not the case, the subscriber's callbacks are called on the thread used
     * by the passed {@link CompletionStage}.
     * If the completion stage redeems {@code null}, it fires the completion event without any item.
     *
     * @param stage the stage, must not be {@code null}
     * @param <T> the type of item
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> completionStage(CompletionStage<? extends T> stage) {
        CompletionStage<? extends T> actual = nonNull(stage, "stage");
        return deferredCompletionStage(() -> actual);
    }

    /**
     * Creates a {@link Multi} from the given {@link CompletionStage} or {@link CompletableFuture}. The future is
     * created by invoking the passed {@link Supplier} <strong>lazily</strong> at subscription time.
     * <p>
     * The produced {@code Multi} emits the item of the passed {@link CompletionStage} followed by the completion
     * event. If the {@link CompletionStage} never completes (or failed), the produced {@link Multi} would not emit
     * an item or a failure.
     * <p>
     * Cancelling the subscription on the produced {@link Multi} cancels the passed {@link CompletionStage}
     * (calling {@link CompletableFuture#cancel(boolean)} on the future retrieved using
     * {@link CompletionStage#toCompletableFuture()}.
     * <p>
     * If the produced stage has already been completed (or failed), the produced {@link Multi} sends the item or
     * failure immediately after subscription. In the case of item, the event is followed by the completion event. If
     * the produced stage is not yet completed, the subscriber's callbacks are called on the thread used
     * by the passed {@link CompletionStage}.
     * <p>
     * If the produced completion stage redeems {@code null}, it fires the completion event without any item.
     * <p>
     * If the supplier throws an exception, a failure event with the exception is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not produce {@code null}
     * @param <T> the type of item
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> deferredCompletionStage(Supplier<? extends CompletionStage<? extends T>> supplier) {
        nonNull(supplier, "supplier");
        return emitter(emitter -> {
            CompletionStage<? extends T> stage;
            try {
                stage = supplier.get();
            } catch (Exception e) {
                emitter.fail(e);
                return;
            }
            if (stage == null) {
                throw new NullPointerException(SUPPLIER_PRODUCED_NULL);
            }
            emitter.onTermination(() -> stage.toCompletableFuture().cancel(false));
            stage.whenComplete((r, f) -> {
                if (f != null) {
                    emitter.fail(f);
                } else if (r != null) {
                    emitter.emit(r);
                }
                emitter.complete();
            });
        }, BackPressureStrategy.LATEST);
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
     * @param <T> the type of item
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
     * Creates an instance of {@link Multi} from the given {@link Uni}.
     * <p>
     * When a subscriber subscribes to the returned {@link Multi} and <strong>request</strong> an item, it subscribes
     * to the given {@link Uni} and the events from this {@link Uni} are propagated to the {@link Multi}:
     * <ul>
     * <li>if the {@link Uni} emits a non-{@code null} item - this item is propagated to the {@link Multi}
     * and followed with the completion event</li>
     * <li>if the {@link Uni} emits a {@code null} item - the {@link Multi} fires the completion event</li>
     * <li>if the {@link Uni} emits a failure, this failure event is propagated by the {@link Multi}</li>
     * </ul>
     * <p>
     * It's important to note that the subscription to the {@link Uni} happens when the subscriber to the produced
     * {@link Multi} requests values, and not at subscription time.
     *
     * @param uni the uni, must not be {@code null}
     * @param <T> the type of item emitted by the resulting {@link Multi} / passed {@link Uni}
     * @return the produced {@link Multi}, never {@code null}
     */
    public <T> Multi<T> uni(Uni<T> uni) {
        return nonNull(uni, "uni").toMulti();
    }

    /**
     * Creates a new {@link Multi} that emits an item immediately after being subscribed to with the specified single
     * (potentially {@code null}) value. The value is retrieved <strong>lazily</strong> at subscription time, using
     * the passed {@link Supplier}. Unlike {@link #deferred(Supplier)}, the supplier produces an item and not a
     * {@link Multi}.
     * <p>
     * If the supplier produces {@code null}, the produced {@link Multi} fires the completion event.
     * If the supplier produces a non-{@code null} item, the produced {@link Multi} fires an item event followed with
     * the completion event.
     * If the supplier throws an exception, a failure event with the exception is fired.
     *
     * @param supplier the item supplier, must not be {@code null}, can produce {@code null}
     * @param <T> the type of item emitted by the produced Multi
     * @return the new {@link Multi}
     */
    public <T> Multi<T> deferredItem(Supplier<? extends T> supplier) {
        Supplier<? extends T> actual = nonNull(supplier, "supplier");
        return emitter(emitter -> {
            T item;
            try {
                item = actual.get();
            } catch (RuntimeException e) {
                // Exception from the supplier, propagate it.
                emitter.fail(e);
                return;
            }
            if (item != null) {
                emitter.emit(item);
            }
            emitter.complete();
        });
    }

    /**
     * Creates a new {@link Multi} that emits the items immediately after being subscribed to. The individual item
     * comes from the {@link Stream} supplied by the given {@link Supplier}. This supplier is called at subscription
     * time.
     * <p>
     * If the supplier produces {@code null}, the produced {@link Multi} fires a failure event.
     * If the supplier produces an empty stream, the produced {@link Multi} fires a completion event.
     * For each items from the supplied stream, an item event is fired. When all the items have been emitted,
     * the completion event is fired.
     * If the supplier throws an exception, a failure event with the exception is fired.
     * the stream is consumed sequentially.
     *
     * @param supplier the item supplier, must not be {@code null}, must not produce {@code null}
     * @param <T> the type of item emitted by the produced Multi
     * @return the new {@link Multi}
     */
    public <T> Multi<T> deferredItems(Supplier<? extends Stream<? extends T>> supplier) {
        Supplier<? extends Stream<? extends T>> actual = nonNull(supplier, "supplier");
        return emitter(emitter -> {
            Stream<? extends T> stream;
            try {
                stream = actual.get();
            } catch (RuntimeException e) {
                // Exception from the supplier, propagate it.
                emitter.fail(e);
                return;
            }
            if (stream == null) {
                emitter.fail(new NullPointerException(SUPPLIER_PRODUCED_NULL));
                return;
            }
            AtomicBoolean failed = new AtomicBoolean();
            stream
                    .sequential()
                    .forEach(it -> {
                        if (it == null) {
                            failed.set(true);
                            emitter.fail(new IllegalArgumentException("The iterable contained a `null` value"));
                            return;
                        }
                        emitter.emit(it);
                    });
            if (!failed.get()) {
                emitter.complete();
            }
        });
    }

    /**
     * Creates a new {@link Multi} that emits an item immediately after being subscribed to with the specified single
     * item. If {@code item} is {@code null} the completion event is fired immediately making the resulting
     * {@link Multi} empty. If {@code item} is non-{@code null}, the item event is fired immediately followed with
     * the completion event.
     *
     * @param item the item, can be {@code null} which would create an empty {@link Multi}
     * @param <T> the type of item emitted by the produced Multi
     * @return the new {@link Multi}
     */
    public <T> Multi<T> item(T item) {
        return deferredItem(() -> item);
    }

    /**
     * Creates a new {@link Multi} that emits the items individually after being subscribed to (according to the
     * subscriber's request).
     * <p>
     * If {@code items} is {@code null}, an {@link IllegalArgumentException} is thrown at call time.
     * If one of the item from {@code items} is {@code null}, a failure event is fired (with an
     * {@link IllegalArgumentException}).
     * When all the items have been emitted, the completion event is fired.
     *
     * @param items the items, must not be {@code null}, must not contain {@code null}
     * @param <T> the type of item emitted by the produced Multi
     * @return the new {@link Multi}
     */
    @SafeVarargs
    public final <T> Multi<T> items(T... items) {
        return iterable(Arrays.asList(nonNull(items, "items")));
    }

    /**
     * Creates a new {@link Multi} that emits the items individually after being subscribed to (according to the
     * subscriber's request).
     * <p>
     * If {@code iterable} is {@code null}, an {@link IllegalArgumentException} is thrown at call time.
     * If one of the item from {@code iterable} is {@code null}, a failure event is fired (with an
     * {@link IllegalArgumentException}).
     * When all the items have been emitted, the completion event is fired.
     *
     * @param iterable the iterable of items, must not be {@code null}, must not contain {@code null}
     * @param <T> the type of item emitted by the produced Multi
     * @return the new {@link Multi}
     */
    public <T> Multi<T> iterable(Iterable<T> iterable) {
        nonNull(iterable, "iterable");
        return deferredItems(() -> StreamSupport.stream(iterable.spliterator(), false));
    }

    /**
     * Creates a new {@link Multi} that emits the items from the passed {@link Stream} individually after being
     * subscribed to (according to the subscriber's request).
     * <p>
     * If {@code items} is {@code null}, an {@link IllegalArgumentException} is thrown at call time.
     * If one of the item from the stream is {@code null}, a failure event is fired (with an
     * {@link IllegalArgumentException}).
     * When all the items have been emitted, the completion event is fired.
     * The stream is consumed sequentially.
     *
     * @param items the items, must not be {@code null}, must not contain {@code null}
     * @param <T> the type of item emitted by the produced Multi
     * @return the new {@link Multi}
     */
    public <T> Multi<T> items(Stream<T> items) {
        Stream<T> stream = nonNull(items, "items");
        return deferredItems(() -> stream);
    }

    /**
     * Creates a new {@link Multi} that emits an item immediately after being subscribed to with the value contained
     * in the given optional if {@link Optional#isPresent()} or empty otherwise.
     *
     * @param optional the optional, must not be {@code null}, an empty optional produces an empty {@link Multi}.
     * @param <T> the type of the produced item
     * @return the new {@link Multi}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public <T> Multi<T> optional(Optional<T> optional) {
        Optional<T> actual = nonNull(optional, "optional");
        return deferredItem(() -> actual.orElse(null));
    }

    /**
     * Creates a new {@link Multi} that emits an item immediately after being subscribed to with the value contained
     * in the optional supplied by {@code supplier}.
     * <p>
     * If the optional is empty, an empty {@link Multi} is produced. Otherwise the contained value is emitted as item,
     * followed with the completion event.
     * Unlike {@link #optional(Optional)}, the passed {@link Supplier} is called lazily at subscription time.
     * <p>
     * If the supplier throws an exception, a failure event with the exception is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not return {@code null}
     * @param <T> the type of the produced item
     * @return the new {@link Multi}
     */
    public <T> Multi<T> deferredOptional(Supplier<Optional<T>> supplier) {
        Supplier<Optional<T>> actual = nonNull(supplier, "supplier");
        return deferredItem(() -> actual.get().orElse(null));
    }

    /**
     * Like {@link #emitter(Consumer, BackPressureStrategy)} with the {@link BackPressureStrategy#BUFFER} strategy.
     *
     * @param consumer the consumer receiving the emitter, must not be {@code null}
     * @param <T> the type of item emitted by the produced Multi
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> emitter(Consumer<MultiEmitter<? super T>> consumer) {
        return emitter(consumer, BackPressureStrategy.BUFFER);
    }

    /**
     * Creates a {@link Multi} deferring the logic to the given consumer. The consumer can be used with callback-based
     * APIs to fire items (non-{@code null}), failure or completion events.
     * <p>
     * Emitting {@code null} value is not supported. Emitting values after having fired a failure or the completion
     * event is a no-op. So subsequent item events are dropped.
     * <p>
     * Using this method, you can produce a {@link Multi} based on listener or callbacks APIs. You register the listener
     * in the consumer and emits the items / failure / completion events when the listener is invoked. Don't forget
     * to unregister the listener on cancellation.
     * <p>
     * If the consume throws an exception, a failure event with the exception is fired.
     *
     * @param consumer callback receiving the {@link MultiEmitter} and events downstream. The callback is
     *        called for each subscriber (at subscription time). Must not be {@code null}
     * @param strategy the back pressure strategy to apply when the downstream subscriber cannot keep up with the
     *        items emitted by the emitter.
     * @param <T> the type of items
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
     * Unlike {@link #deferredItem(Supplier)}, the supplier produces an {@link Multi} (and not an item).
     * <p>
     * If the supplier throws an exception, a failure event with the exception is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not produce {@code null}
     * @param <T> the type of item
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
     * @param <T> the virtual type of item used by the {@link Multi}, must be explicitly set as in
     *        {@code Multi.<String>failed(exception);}
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> failure(Throwable failure) {
        Throwable exception = nonNull(failure, "failure");
        return deferredFailure(() -> exception);
    }

    /**
     * Creates a {@link Multi} that emits a {@code failure} event produced using the passed supplier immediately after
     * being subscribed to. The supplier is called at subscription time, and produces an instance of {@link Throwable}.
     * If the supplier throws an exception, a {@code failure} event is fired with this exception.
     * If the supplier produces {@code null}, a {@code failure} event is fired with a {@link NullPointerException}.
     *
     * @param supplier the supplier producing the failure, must not be {@code null}, must not produce {@code null}
     * @param <T> the virtual type of item used by the {@link Multi}, must be explicitly set as in
     *        {@code Multi.<String>failed(exception);}
     * @return the produced {@link Multi}
     */
    public <T> Multi<T> deferredFailure(Supplier<Throwable> supplier) {
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
     * @param <T> the virtual type of item
     * @return a never emitting {@link Multi}
     */
    @SuppressWarnings("unchecked")
    public <T> Multi<T> nothing() {
        return (Multi<T>) MultiNever.INSTANCE;
    }

    /**
     * Creates a {@link Multi} that fires the completion event without having emitted any items.
     * An empty {@link Multi} does not fires a failure event either.
     *
     * @param <T> the virtual type of item
     * @return an empty {@link Multi}
     */
    @SuppressWarnings("unchecked")
    public <T> Multi<T> empty() {
        return (Multi<T>) MultiEmpty.INSTANCE;
    }

    /**
     * Creates a {@link Multi} that emits {@code long} items (ticks) starting with 0 and incrementing at
     * specified time intervals.
     * <p>
     * Be aware that if the subscriber does not request enough item in time, a back pressure failure is fired.
     * The produced {@link Multi} never completes until cancellation by the subscriber.
     * <p>
     * The callbacks are invoked on the executor passed in {@link MultiTimePeriod#onExecutor(Executor)}.
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
     * @param endExclusive the end integer (exclusive)
     * @return the {@link Multi} emitting the items
     */
    public Multi<Integer> range(int startInclusive, int endExclusive) {
        if (endExclusive <= startInclusive) {
            throw new IllegalArgumentException("end must be greater than start");
        }
        return Multi.createFrom().iterable(() -> IntStream.range(startInclusive, endExclusive).iterator());
    }

    //    public <T, X> Multi<T> converterOf(X instance) {
    //        return MultiAdaptFrom.adaptFrom(instance);
    //    }
}
