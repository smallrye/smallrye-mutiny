package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.*;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.MultiConverter;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.operators.multi.builders.*;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.SafeSubscriber;

/**
 * Group methods allowing to create {@link Multi} instances from various sources.
 */
public class MultiCreate {

    public static final MultiCreate INSTANCE = new MultiCreate();

    private MultiCreate() {
        // avoid direct instantiation.
    }

    /**
     * Creates a new {@link Multi} from the passed instance with the passed converter.
     *
     * @param converter performs the type conversion
     * @param instance instance to convert from
     * @param <I> the type being converted from
     * @param <T> the type for the {@link Multi}
     * @return the created {@link Multi}
     */
    @CheckReturnValue
    public <I, T> Multi<T> converter(MultiConverter<I, T> converter, I instance) {
        return Infrastructure.onMultiCreation(converter.from(instance));
    }

    /**
     * Creates a {@link Multi} from the given {@link CompletionStage} or {@link CompletableFuture}.
     * The produced {@code Multi} emits the item of the passed {@link CompletionStage} and then fires the completion
     * event. If the {@link CompletionStage} never completes (or fails), the produced {@link Multi} would not emit
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
    @CheckReturnValue
    public <T> Multi<T> completionStage(CompletionStage<? extends T> stage) {
        CompletionStage<? extends T> actual = nonNull(stage, "stage");
        return completionStage(() -> actual);
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
    @CheckReturnValue
    public <T> Multi<T> completionStage(Supplier<? extends CompletionStage<? extends T>> supplier) {
        Supplier<? extends CompletionStage<? extends T>> actual = Infrastructure
                .decorate(nonNull(supplier, "supplier"));
        return emitter(emitter -> {
            CompletionStage<? extends T> stage;
            try {
                stage = actual.get();
            } catch (Throwable e) {
                emitter.fail(e);
                return;
            }
            if (stage == null) {
                throw new NullPointerException(SUPPLIER_PRODUCED_NULL);
            }
            emitter.onTermination(() -> stage.toCompletableFuture().cancel(false));
            stage.whenComplete((r, f) -> {
                if (f != null) {
                    if (f instanceof CompletionException) {
                        emitter.fail(f.getCause());
                    } else {
                        emitter.fail(f);
                    }
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
     * It is assumed that the {@link Publisher} is fully compliant with the Reactive Streams protocol and passes the TCK.
     * If this is not the case use {@link #publisher(Publisher)} instead.
     * <p>
     * When a subscriber subscribes to the produced {@link Multi}, it subscribes to the {@link Publisher} and delegate
     * the requests. Note that each Multi's subscriber would produce a new subscription.
     * <p>
     * If the Multi's observer cancels its subscription, the subscription to the {@link Publisher} is also cancelled.
     * <p>
     * If a {@code Multi} is passed as parameter, this {@code Multi} is returned.
     *
     * @param publisher the publisher, must not be {@code null}
     * @param <T> the type of item
     * @return the produced {@link Multi}
     * @see #publisher(Publisher)
     */
    @CheckReturnValue
    public <T> Multi<T> safePublisher(Publisher<T> publisher) {
        Publisher<T> actual = nonNull(publisher, "publisher");

        if (publisher instanceof Multi) {
            // Should not call onMultiCreation - it should have been done already.
            return (Multi<T>) publisher;
        }

        return Infrastructure.onMultiCreation(new AbstractMulti<T>() {
            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                actual.subscribe(subscriber);
            }
        });
    }

    /**
     * Creates a {@link Multi} from the passed {@link Publisher}.
     * <p>
     * The {@link Publisher} is not assumed to be fully compliant with the Reactive Streams TCK, hence it is wrapped
     * around a subscriber that enforces the Reactive Streams protocol.
     * If you know the {@link Publisher} is safe then you should use {@link #safePublisher(Publisher)} instead.
     * <p>
     * When a subscriber subscribes to the produced {@link Multi}, it subscribes to the {@link Publisher} and delegate
     * the requests. Note that each Multi's subscriber would produce a new subscription.
     * <p>
     * If the Multi's observer cancels its subscription, the subscription to the {@link Publisher} is also cancelled.
     * <p>
     * If a {@code Multi} is passed as parameter, this {@code Multi} is returned.
     *
     * @param publisher the publisher, must not be {@code null}
     * @param <T> the type of item
     * @return the produced {@link Multi}
     * @see #safePublisher(Publisher)
     */
    @CheckReturnValue
    public <T> Multi<T> publisher(Publisher<T> publisher) {
        Publisher<T> actual = nonNull(publisher, "publisher");

        if (publisher instanceof Multi) {
            // Should not call onMultiCreation - it should have been done already.
            return (Multi<T>) publisher;
        }

        return Infrastructure.onMultiCreation(new AbstractMulti<T>() {
            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                actual.subscribe(new SafeSubscriber<>(subscriber));
            }
        });
    }

    /**
     * Creates an never of {@link Multi} from the given {@link Uni}.
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
    @CheckReturnValue
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
    @CheckReturnValue
    public <T> Multi<T> item(Supplier<? extends T> supplier) {
        Supplier<? extends T> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));

        return emitter(emitter -> {
            T item;
            try {
                item = actual.get();
            } catch (Throwable err) {
                // Exception from the supplier, propagate it.
                emitter.fail(err);
                return;
            }
            if (item != null) {
                emitter.emit(item);
            }
            emitter.complete();
        });
    }

    /**
     * Creates a new {@link Multi} that emits the items immediately after being subscribed to. The individual items
     * come from the {@link Stream} supplied by the given {@link Supplier}. This supplier is called at subscription
     * time.
     * <p>
     * If the supplier produces {@code null}, the produced {@link Multi} fires a failure event.
     * If the supplier produces an empty stream, the produced {@link Multi} fires a completion event.
     * For each item from the supplied stream, an item event is fired. When all the items have been emitted,
     * the completion event is fired.
     * If the supplier throws an exception, a failure event with the exception is fired.
     * The stream is consumed sequentially.
     *
     * @param supplier the item supplier, must not be {@code null}, must not produce {@code null}
     * @param <T> the type of item emitted by the produced Multi
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public <T> Multi<T> items(Supplier<? extends Stream<? extends T>> supplier) {
        Supplier<? extends Stream<? extends T>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return Infrastructure.onMultiCreation(new StreamBasedMulti<>(actual));
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
    @CheckReturnValue
    public <T> Multi<T> item(T item) {
        return item(() -> item);
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
    @CheckReturnValue
    public final <T> Multi<T> items(T... items) {
        return Infrastructure.onMultiCreation(new CollectionBasedMulti<>(nonNull(items, "items")));
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
    @CheckReturnValue
    public <T> Multi<T> iterable(Iterable<T> iterable) {
        return Infrastructure.onMultiCreation(new IterableBasedMulti<>(nonNull(iterable, "iterable")));
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
    @CheckReturnValue
    public <T> Multi<T> items(Stream<T> items) {
        Stream<T> stream = nonNull(items, "items");
        return items(() -> stream);
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
    @CheckReturnValue
    public <T> Multi<T> optional(Optional<T> optional) {
        Optional<T> actual = nonNull(optional, "optional");
        return item(() -> actual.orElse(null));
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
    @CheckReturnValue
    public <T> Multi<T> optional(Supplier<Optional<T>> supplier) {
        Supplier<Optional<T>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return item(() -> actual.get().orElse(null));
    }

    /**
     * Like {@link #emitter(Consumer, BackPressureStrategy)} with the {@link BackPressureStrategy#BUFFER} strategy.
     * <p>
     * Note that to create hot streams, you should use a
     * {@link io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor}.
     *
     * @param consumer the consumer receiving the emitter, must not be {@code null}
     * @param <T> the type of item emitted by the produced Multi
     * @return the produced {@link Multi}
     */
    @CheckReturnValue
    public <T> Multi<T> emitter(Consumer<MultiEmitter<? super T>> consumer) {
        // Decoration happens in `emitter`
        return emitter(consumer, BackPressureStrategy.BUFFER);
    }

    /**
     * Like {@link #emitter(Consumer)} with the {@link BackPressureStrategy#BUFFER} strategy and the given buffer size.
     * <p>
     * Note that to create hot streams, you should use a
     * {@link io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor}.
     * <p>
     * If the buffer is full, a {@link java.nio.BufferOverflowException} in propagated downstream.
     *
     * @param consumer the consumer receiving the emitter, must not be {@code null}
     * @param bufferSize the buffer size, must be strictly positive
     * @param <T> the type of item emitted by the produced Multi
     * @return the produced {@link Multi}
     */
    @CheckReturnValue
    public <T> Multi<T> emitter(Consumer<MultiEmitter<? super T>> consumer, int bufferSize) {
        Consumer<MultiEmitter<? super T>> actual = Infrastructure.decorate(nonNull(consumer, "consumer"));
        return Infrastructure.onMultiCreation(new EmitterBasedMulti<>(actual, BackPressureStrategy.BUFFER,
                positive(bufferSize, "bufferSize")));
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
     * If the consumer throws an exception, a failure event with the exception is fired.
     * <p>
     * Note that to create hot streams, you should use a
     * {@link io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor}.
     *
     * @param consumer callback receiving the {@link MultiEmitter} and events downstream. The callback is
     *        called for each subscriber (at subscription time). Must not be {@code null}
     * @param strategy the back pressure strategy to apply when the downstream subscriber cannot keep up with the
     *        items emitted by the emitter.
     * @param <T> the type of items emitted by the emitter. Must not be {@code null}
     * @return the produced {@link Multi}
     */
    @CheckReturnValue
    public <T> Multi<T> emitter(Consumer<MultiEmitter<? super T>> consumer, BackPressureStrategy strategy) {
        Consumer<MultiEmitter<? super T>> actual = Infrastructure.decorate(nonNull(consumer, "consumer"));
        return Infrastructure.onMultiCreation(new EmitterBasedMulti<>(actual, nonNull(strategy, "strategy")));
    }

    /**
     * Creates a {@link Multi} that {@link Supplier#get supplies} an {@link Multi} to subscribe to for each
     * {@link Subscriber}. The supplier is called at subscription time.
     * <p>
     * In practice, it defers the {@link Multi} creation at subscription time and allows each subscriber to get different
     * {@link Multi}. So, it does not create the {@link Multi} until an {@link Subscriber subscriber} subscribes, and
     * creates a fresh {@link Multi} for each subscriber.
     * <p>
     * Unlike {@link #item(Supplier)}, the supplier produces an {@link Multi} (and not an item).
     * <p>
     * If the supplier throws an exception, a failure event with the exception is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not produce {@code null}
     * @param <T> the type of item
     * @return the produced {@link Multi}
     */
    @CheckReturnValue
    public <T> Multi<T> deferred(Supplier<Multi<? extends T>> supplier) {
        Supplier<Multi<? extends T>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return Infrastructure.onMultiCreation(new DeferredMulti<>(actual));
    }

    /**
     * Creates a {@link Multi} using {@link Function#apply(Object)} on the subscription-bound {@link Context}
     * (the mapper is called at subscription time).
     * <p>
     * This method is semantically equivalent to {@link #deferred(Supplier)}, except that it passes a context.
     *
     * @param mapper the mapper, must not be {@code null}, must not produce {@code null}
     * @param <T> the type of the item
     * @return the produced {@link Multi}
     */
    @CheckReturnValue
    public <T> Multi<T> context(Function<Context, Multi<? extends T>> mapper) {
        Function<Context, Multi<? extends T>> actual = Infrastructure.decorate(nonNull(mapper, "mapper"));
        return Infrastructure.onMultiCreation(new DeferredMultiWithContext<>(actual));
    }

    /**
     * Creates a {@link Multi} that emits a {@code failure} event immediately after being subscribed to.
     *
     * @param failure the failure to be fired, must not be {@code null}
     * @param <T> the virtual type of item used by the {@link Multi}, must be explicitly set as in
     *        {@code Multi.<String>failed(exception);}
     * @return the produced {@link Multi}
     */
    @CheckReturnValue
    public <T> Multi<T> failure(Throwable failure) {
        Throwable exception = nonNull(failure, "failure");
        return failure(() -> exception);
    }

    /**
     * Creates a {@link Multi} that emits a {@code failure} event produced using the passed supplier immediately after
     * being subscribed to. The supplier is called at subscription time, and produces an never of {@link Throwable}.
     * If the supplier throws an exception, a {@code failure} event is fired with this exception.
     * If the supplier produces {@code null}, a {@code failure} event is fired with a {@link NullPointerException}.
     *
     * @param supplier the supplier producing the failure, must not be {@code null}, must not produce {@code null}
     * @param <T> the virtual type of item used by the {@link Multi}, must be explicitly set as in
     *        {@code Multi.<String>failed(exception);}
     * @return the produced {@link Multi}
     */
    @CheckReturnValue
    public <T> Multi<T> failure(Supplier<Throwable> supplier) {
        Supplier<Throwable> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return Infrastructure.onMultiCreation(new FailedMulti<>(actual));
    }

    /**
     * Creates a {@link Multi} that will never fire any events.
     *
     * @param <T> the virtual type of item
     * @return a never emitting {@link Multi}
     */
    @CheckReturnValue
    public <T> Multi<T> nothing() {
        return Infrastructure.onMultiCreation(NeverMulti.never());
    }

    /**
     * Creates a {@link Multi} that fires the completion event without having emitted any items.
     * An empty {@link Multi} does not fires a failure event either.
     *
     * @param <T> the virtual type of item
     * @return an empty {@link Multi}
     */
    @CheckReturnValue
    public <T> Multi<T> empty() {
        return Infrastructure.onMultiCreation(EmptyMulti.empty());
    }

    /**
     * Creates a {@link Multi} that emits {@code long} items (ticks) starting with 0 and incrementing at
     * specified time intervals.
     * <p>
     * The <em>timer</em> starts at the first request. Once this request is received, the produced stream is a hot
     * stream.
     * <p>
     * Be aware that if the subscriber does not request enough items in time, a back pressure failure is fired.
     * The produced {@link Multi} never completes until cancellation by the subscriber.
     * <p>
     * The callbacks are invoked on the executor passed in {@link MultiTimePeriod#onExecutor(ScheduledExecutorService)}.
     *
     * @return the object to configure the time period (initial delay, executor, interval)
     */
    @CheckReturnValue
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
    @CheckReturnValue
    public Multi<Integer> range(int startInclusive, int endExclusive) {
        if (endExclusive <= startInclusive) {
            throw new IllegalArgumentException("end must be greater than start");
        }
        return Multi.createFrom().iterable(() -> IntStream.range(startInclusive, endExclusive).iterator());
    }

    /**
     * Creates a {@link Multi} from a <em>resource</em>, generated by a supplier function called for each individual
     * {@link Subscriber}, while streaming the items from a {@link Publisher Publisher/Multi} created from the resource.
     * <p>
     * This method gets a <em>resource</em> and creates a {@link Publisher} from this resource (by calling the
     * {@code streamSupplier} function). The subscriber receives the items from this {@link Publisher}. When the stream
     * completes, fails or when the subscriber cancels the subscription, a finalizer is called to <em>close</em> the
     * resource. This cleanup process can be either synchronous and asynchronous, as well as distinct for each type of
     * event.
     * <p>
     * This method can be seen as a reactive version of the "try/finally" construct.
     *
     * @param resourceSupplier a supplier called for each subscriber to generate the resource, must not be {@code null}.
     * @param streamSupplier a function returning the stream for the given resource instance, must not be {@code null}.
     * @param <R> the type of the resource.
     * @param <I> the type of items emitted by the stream produced by the {@code streamSupplier}.
     * @return an object to configure the <em>finalizers</em>.
     */
    @CheckReturnValue
    public <R, I> MultiResource<R, I> resource(Supplier<? extends R> resourceSupplier,
            Function<? super R, ? extends Publisher<I>> streamSupplier) {
        Supplier<? extends R> actual = Infrastructure.decorate(nonNull(resourceSupplier, "resourceSupplier"));
        Function<? super R, ? extends Publisher<I>> actualStreamSupplier = Infrastructure
                .decorate(nonNull(streamSupplier, "streamSupplier"));
        return new MultiResource<>(actual, actualStreamSupplier);
    }

    /**
     * Creates a {@link Multi} from a <em>resource</em>, generated by a supplier function called for each individual
     * {@link Subscriber}, while streaming the items from a {@link Publisher Publisher/Multi} created from the resource.
     * <p>
     * Unlike {@link #resource(Supplier, Function)}, the {@code Supplier} produces a {@link Uni}. So, the actual
     * <em>resource</em> can be resolved asynchronously.
     * </p>
     * This method gets a <em>resource</em> and creates a {@link Publisher} from this resource (by calling the
     * {@code streamSupplier} function once the {@code Uni} emits the resource instance). The subscriber receives the
     * items from this {@link Publisher}. When the stream completes, fails or when the subscriber cancels the
     * subscription, a finalizer is called to <em>close</em> the resource. This cleanup process can be either
     * synchronous and asynchronous, as well as distinct for each type of event.
     * <p>
     * If the Uni produced by the {@code resourceSupplier} emits a failure, the failure is propagated downstream.
     * If the Uni produced by the {@code resourceSupplier} does not emit an item before downstream cancellation, the
     * resource creation is cancelled.
     * <p>
     * This method can be seen as a reactive version of the "try/finally" construct.
     *
     * @param resourceSupplier a supplier called for each subscriber to generate the resource, must not be {@code null}.
     *        The supplier produces a {@link Uni} emitting the resource.
     * @param streamSupplier a function returning the stream for the given resource instance, must not be {@code null}.
     * @param <R> the type of the resource.
     * @param <I> the type of items emitted by the stream produced by the {@code streamSupplier}.
     * @return an object to configure the <em>finalizers</em>.
     */
    @CheckReturnValue
    public <R, I> MultiResourceUni<R, I> resourceFromUni(Supplier<Uni<R>> resourceSupplier,
            Function<? super R, ? extends Publisher<I>> streamSupplier) {
        Supplier<Uni<R>> actual = Infrastructure.decorate(nonNull(resourceSupplier, "resourceSupplier"));
        Function<? super R, ? extends Publisher<I>> actualStreamSupplier = Infrastructure
                .decorate(nonNull(streamSupplier, "streamSupplier"));
        return new MultiResourceUni<>(actual, actualStreamSupplier);
    }

    /**
     * Creates a {@link Multi} from on some initial state and a generator function.
     * <p>
     * The generator function accepts the current state and a {@link GeneratorEmitter} to emit items, failures and completion
     * events.
     * The function shall return the new state which will be used for the next item generation, if any.
     * The state can be {@code null}, but emitted items cannot be {@code null}.
     * A failure is propagated downstream if the function throws an exception.
     * <p>
     * Items are being generated based on subscription requests.
     * Requesting {@link Long#MAX_VALUE} items can possibly make for an infinite stream unless the generator function calls
     * {@link GeneratorEmitter#complete()} at some point.
     *
     * @param initialStateSupplier a supplier for the initial state, must not be {@code null} but can supply {@code null}
     * @param generator the generator function, returns the new state for the next item generation
     * @param <S> the state type
     * @param <T> the items type
     * @return a new {@link Multi}
     */
    @CheckReturnValue
    public <S, T> Multi<T> generator(Supplier<S> initialStateSupplier,
            BiFunction<S, GeneratorEmitter<? super T>, S> generator) {
        Supplier<S> actualStateSupplier = Infrastructure
                .decorate(nonNull(initialStateSupplier, "initialStateSupplier"));
        BiFunction<S, GeneratorEmitter<? super T>, S> actualGenerator = Infrastructure
                .decorate(nonNull(generator, "generator"));
        return new GeneratorBasedMulti<>(actualStateSupplier, actualGenerator);
    }
}
