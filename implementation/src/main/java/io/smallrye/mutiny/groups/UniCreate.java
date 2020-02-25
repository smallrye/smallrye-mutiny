package io.smallrye.mutiny.groups;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.UniConverter;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.*;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.subscription.UniSubscriber;

/**
 * Group methods allowing to create {@link Uni} instances from various sources.
 */
public class UniCreate {

    public static final UniCreate INSTANCE = new UniCreate();

    private UniCreate() {
        // avoid direct instantiation.
    }

    /**
     * Creates a new {@link Uni} from the passed instance with the passed converter.
     *
     * @param converter performs the type conversion
     * @param instance instance to convert from
     * @param <I> the type being converted from
     * @param <T> the type for the {@link Uni}
     * @return created {@link Uni}
     */
    public <I, T> Uni<T> converter(UniConverter<I, T> converter, I instance) {
        return converter.from(instance);
    }

    /**
     * Creates a {@link Uni} from the given {@link CompletionStage} or {@link CompletableFuture}.
     * The produced {@code Uni} emits the item of the passed {@link CompletionStage}. If the {@link CompletionStage}
     * never completes (or failed), the produced {@link Uni} would not emit the {@code item} or {@code failure}
     * events.
     * <p>
     * Cancelling the subscription on the produced {@link Uni} cancels the passed {@link CompletionStage}
     * (calling {@link CompletableFuture#cancel(boolean)} on the future retrieved using
     * {@link CompletionStage#toCompletableFuture()}.
     * <p>
     * If the stage has already been completed (or failed), the produced {@link Uni} sends the item or failure
     * immediately after subscription. If it's not the case, the subscriber's callbacks are called on the thread used
     * by the passed {@link CompletionStage}.
     *
     * @param stage the stage, must not be {@code null}
     * @param <T> the type of item
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> completionStage(CompletionStage<? extends T> stage) {
        CompletionStage<? extends T> actual = ParameterValidation.nonNull(stage, "stage");
        return completionStage(() -> actual);
    }

    /**
     * Creates a {@link Uni} from the given {@link CompletionStage} or {@link CompletableFuture}.
     * The produced {@code Uni} emits the item of the passed {@link CompletionStage}. If the {@link CompletionStage}
     * never completes (or failed), the produced {@link Uni} would not emit the {@code item} or {@code failure}
     * events.
     * <p>
     * Cancelling the subscription on the produced {@link Uni} cancels the passed {@link CompletionStage}
     * (calling {@link CompletableFuture#cancel(boolean)} on the future retrieved using
     * {@link CompletionStage#toCompletableFuture()}.
     * <p>
     * If the stage has already been completed (or failed), the produced {@link Uni} sends the item or failure
     * immediately after subscription. If it's not the case, the subscriber's callbacks are called on the thread used
     * by the passed {@link CompletionStage}.
     * <p>
     * This variant of {@link #completionStage(CompletionStage)} allows passing a state supplier. This supplier allows
     * sharing some <em>state</em> between the subscribers. It is particularly useful when using {@link Uni#repeat()}
     * as you can pass a shared state (for example a page counter, like an AtomicInteger, if you implement pagination).
     * The state supplier is called once, during the first subscription. Note that the mapper is called for every
     * subscription.
     * <p>
     * The state supplier should produce a container wrapping the shared state. This shared state must be thread-safe.
     *
     * @param stateSupplier the state supplier, must not return {@code null}, must not be {@code null}
     * @param mapper the taking the shared state and producing the completion stage.
     * @param <T> the type of item
     * @param <S> the type of the state
     * @return the produced {@link Uni}
     */
    public <T, S> Uni<T> completionStage(Supplier<S> stateSupplier,
            Function<S, ? extends CompletionStage<? extends T>> mapper) {
        ParameterValidation.nonNull(stateSupplier, "stateSupplier");
        ParameterValidation.nonNull(mapper, "mapper");

        // Flag checking that the state supplier is only called once.
        AtomicBoolean once = new AtomicBoolean();
        // The shared state container.
        AtomicReference<S> state = new AtomicReference<>();

        return Uni.createFrom().deferred(() -> {
            try {
                invokeOnce(once, state, stateSupplier);
            } catch (Throwable e) {
                return Uni.createFrom().failure(e);
            }

            S sharedState = state.get();
            if (sharedState == null) {
                // The state supplier failed or produced null.
                return Uni.createFrom().failure(new IllegalStateException("Invalid shared state"));
            }
            return Uni.createFrom().completionStage(mapper.apply(sharedState));
        });
    }

    /**
     * Creates a {@link Uni} from the given {@link CompletionStage} or {@link CompletableFuture}. The future is
     * created by invoking the passed {@link Supplier} <strong>lazily</strong> at subscription time.
     * <p>
     * The produced {@code Uni} emits the item of the passed {@link CompletionStage}. If the {@link CompletionStage}
     * never completes (or failed), the produced {@link Uni} would not emit an item or a failure.
     * <p>
     * Cancelling the subscription on the produced {@link Uni} cancels the passed {@link CompletionStage}
     * (calling {@link CompletableFuture#cancel(boolean)} on the future retrieved using
     * {@link CompletionStage#toCompletableFuture()}.
     * <p>
     * If the produced stage has already been completed (or failed), the produced {@link Uni} sends the item or failure
     * immediately after subscription. If it's not the case the subscriber's callbacks are called on the thread used
     * by the passed {@link CompletionStage}.
     * <p>
     * If the supplier throws an exception, a failure event with the exception is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not produce {@code null}
     * @param <T> the type of item
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> completionStage(Supplier<? extends CompletionStage<? extends T>> supplier) {
        return Infrastructure
                .onUniCreation(new UniCreateFromCompletionStage<>(ParameterValidation.nonNull(supplier, "supplier")));
    }

    /**
     * Creates a {@link Uni} from the passed {@link Publisher}.
     * <p>
     * The produced {@link Uni} emits the <strong>first</strong> item/value emitted by the passed {@link Publisher}.
     * If the publisher emits multiple values, others are dropped. If the publisher emits a failure after a value, the
     * failure is dropped. If the publisher emits the completion signal before having emitted a value, the produced
     * {@link Uni} emits a {@code null} item event.
     * <p>
     * When a subscriber subscribes to the produced {@link Uni}, it subscribes to the {@link Publisher} and requests
     * {@code 1} item. When the first item is received, the subscription is cancelled. Note that each Uni's subscriber
     * would produce a new subscription.
     * <p>
     * If the Uni's observer cancels its subscription, the subscription to the {@link Publisher} is also cancelled.
     *
     * @param publisher the publisher, must not be {@code null}
     * @param <T> the type of item
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> publisher(Publisher<? extends T> publisher) {
        Publisher<? extends T> actual = ParameterValidation.nonNull(publisher, "publisher");
        return Infrastructure.onUniCreation(new UniCreateFromPublisher<>(actual));
    }

    /**
     * Creates a new {@link Uni} that completes immediately after being subscribed to with the specified (potentially
     * {@code null}) value. The item is retrieved <strong>lazily</strong> at subscription time, using the passed
     * {@link Supplier}. Unlike {@link #deferred(Supplier)}, the supplier produces an item and not an {@link Uni}.
     * <p>
     * If the supplier produces {@code null}, {@code null} is used as item event.
     * If the supplier throws an exception, a failure event with the exception is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the item supplier, must not be {@code null}, can produce {@code null}
     * @param <T> the type of item
     * @return the new {@link Uni}
     */
    public <T> Uni<T> item(Supplier<? extends T> supplier) {
        Supplier<? extends T> actual = ParameterValidation.nonNull(supplier, "supplier");
        return emitter(emitter -> {
            T item;
            try {
                item = actual.get();
            } catch (RuntimeException e) {
                // Exception from the supplier, propagate it.
                emitter.fail(e);
                return;
            }
            emitter.complete(item);
        });
    }

    /**
     * Creates a new {@link Uni} that completes immediately after being subscribed to with the specified (potentially
     * {@code null}) value. The item is retrieved <strong>lazily</strong> at subscription time, using the passed
     * {@link Supplier}. Unlike {@link #deferred(Supplier)}, the supplier produces an item and not an {@link Uni}.
     * <p>
     * This variant of {@link #item(Supplier)} allows passing a state supplier. This supplier allows
     * sharing some <em>state</em> between the subscribers. It is particularly useful when using {@link Uni#repeat()}
     * as you can pass a shared state (for example a page counter, like an AtomicInteger, if you implement pagination).
     * The state supplier is called once, during the first subscription. Note that the mapper is called for every
     * subscription.
     * <p>
     * The state supplier should produce a container wrapping the shared state. This shared state must be thread-safe.
     *
     * @param stateSupplier the state supplier, must not return {@code null}, must not be {@code null}
     * @param mapper the taking the shared state and producing the item.
     * @param <T> the type of item
     * @param <S> the type of the state
     * @return the produced {@link Uni}
     */
    public <T, S> Uni<T> item(Supplier<S> stateSupplier,
            Function<S, ? extends T> mapper) {
        ParameterValidation.nonNull(stateSupplier, "stateSupplier");
        ParameterValidation.nonNull(mapper, "mapper");
        // Flag checking that the state supplier is only called once.
        AtomicBoolean once = new AtomicBoolean();
        // The shared state container.
        AtomicReference<S> state = new AtomicReference<>();

        return Uni.createFrom().deferred(() -> {
            try {
                invokeOnce(once, state, stateSupplier);
            } catch (Throwable e) {
                return Uni.createFrom().failure(e);
            }

            S sharedState = state.get();
            if (sharedState == null) {
                // The state supplier failed or produced null.
                return Uni.createFrom().failure(new IllegalStateException("Invalid shared state"));
            }
            return Uni.createFrom().item(mapper.apply(sharedState));
        });
    }

    /**
     * Creates a new {@link Uni} that completes immediately after being subscribed to with the specified (potentially
     * {@code null}) item.
     *
     * @param item the item, can be {@code null}
     * @param <T> the type of item
     * @return the new {@link Uni}
     */
    public <T> Uni<T> item(T item) {
        return item(() -> item);
    }

    /**
     * Creates a new {@link Uni} that completes immediately after being subscribed to with a {@code null} item.
     *
     * @return the new {@link Uni} with a {@code null} item
     */
    public Uni<Void> voidItem() {
        return item(() -> null);
    }

    /**
     * Creates a new {@link Uni} that completes immediately after being subscribed to with the item based on the value
     * contained in the given optional if {@link Optional#isPresent()} or {@code null} otherwise.
     *
     * @param optional the optional, must not be {@code null}
     * @param <T> the type of the produced item
     * @return the new {@link Uni}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public <T> Uni<T> optional(Optional<T> optional) {
        Optional<T> actual = ParameterValidation.nonNull(optional, "optional");
        return optional(() -> actual);
    }

    /**
     * Creates a new {@link Uni} that completes immediately after being subscribed to with the item based on the value
     * contained in the given optional if {@link Optional#isPresent()} or {@code null} otherwise. Unlike
     * {@link #optional(Optional)}, the passed {@link Supplier} is called lazily at subscription time.
     * <p>
     * If the supplier throws an exception, a failure event with the exception is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not return {@code null}
     * @param <T> the type of the produced item
     * @return the new {@link Uni}
     */
    public <T> Uni<T> optional(Supplier<Optional<T>> supplier) {
        Supplier<Optional<T>> actual = ParameterValidation.nonNull(supplier, "supplier");
        return item(() -> actual.get().orElse(null));
    }

    /**
     * Creates a {@link Uni} deferring the logic to the given consumer. The consumer can be used with callback-based
     * APIs to fire at most one item (potentially {@code null}), or a failure event.
     * <p>
     * Using this method, you can produce a {@link Uni} based on listener or callbacks APIs. You register the listener
     * in the consumer and emits the item / failure events when the listener is invoked. Don't forget to unregister
     * the listener on cancellation.
     * <p>
     * Note that the emitter only forwards the first event, subsequent events are dropped.
     * <p>
     * If the consumer throws an exception, a failure event with the exception is fired if the first event was already
     * fired.
     *
     * @param consumer callback receiving the {@link UniEmitter} and events downstream. The callback is
     *        called for each subscriber (at subscription time). Must not be {@code null}
     * @param <T> the type of item
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> emitter(Consumer<UniEmitter<? super T>> consumer) {
        Consumer<UniEmitter<? super T>> actual = ParameterValidation.nonNull(consumer, "consumer");
        return Infrastructure.onUniCreation(new UniCreateWithEmitter<>(actual));
    }

    /**
     * Creates a {@link Uni} deferring the logic to the given consumer. The consumer can be used with callback-based
     * APIs to fire at most one item (potentially {@code null}), or a failure event.
     * <p>
     * Using this method, you can produce a {@link Uni} based on listener or callbacks APIs. You register the listener
     * in the consumer and emits the item / failure events when the listener is invoked. Don't forget to unregister
     * the listener on cancellation.
     * <p>
     * Note that the emitter only forwards the first event, subsequent events are dropped.
     * <p>
     * If the consumer throws an exception, a failure event with the exception is fired if the first event was already
     * fired.
     * This variant of {@link #emitter(Consumer)} allows passing a state supplier. This supplier allows
     * sharing some <em>state</em> between the subscribers. It is particularly useful when using {@link Uni#repeat()}
     * as you can pass a shared state (for example a page counter, like an AtomicInteger, if you implement pagination).
     * The state supplier is called once, during the first subscription. Note that the mapper is called for every
     * subscription.
     * <p>
     * The state supplier should produce a container wrapping the shared state. This shared state must be thread-safe.
     *
     * @param stateSupplier the state supplier, must not return {@code null}, must not be {@code null}
     * @param consumer callback receiving the {@link UniEmitter} and events downstream. The callback is
     *        called for each subscriber (at subscription time). Must not be {@code null}
     * @param <T> the type of item
     * @param <S> the type of the state
     * @return the produced {@link Uni}
     */
    public <T, S> Uni<T> emitter(Supplier<S> stateSupplier, BiConsumer<S, UniEmitter<? super T>> consumer) {
        BiConsumer<S, UniEmitter<? super T>> actual = ParameterValidation.nonNull(consumer, "consumer");
        ParameterValidation.nonNull(stateSupplier, "stateSupplier");
        // Flag checking that the state supplier is only called once.
        AtomicBoolean once = new AtomicBoolean();
        // The shared state container.
        AtomicReference<S> state = new AtomicReference<>();

        return Uni.createFrom().deferred(() -> {
            try {
                invokeOnce(once, state, stateSupplier);
            } catch (Throwable e) {
                return Uni.createFrom().failure(e);
            }
            S sharedState = state.get();
            if (sharedState == null) {
                // The state supplier failed or produced null.
                return Uni.createFrom().failure(new IllegalStateException("Invalid shared state"));
            }
            return Uni.createFrom().emitter(e -> actual.accept(sharedState, e));
        });
    }

    private <S> void invokeOnce(AtomicBoolean once, AtomicReference<S> container, Supplier<S> supplier) {
        if (!once.getAndSet(true)) {
            S sharedState = null;
            try {
                sharedState = supplier.get();
                if (sharedState == null) {
                    throw new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL);
                }
            } finally {
                container.set(sharedState);
            }
        }
    }

    /**
     * Creates a {@link Uni} that {@link Supplier#get supplies} an {@link Uni} to subscribe to for each
     * {@link UniSubscriber}. The supplier is called at subscription time.
     * <p>
     * In practice, it defers the {@link Uni} creation at subscription time and allows each subscriber to get different
     * {@link Uni}. So, it does not create the {@link Uni} until an {@link UniSubscriber subscriber} subscribes, and
     * creates a fresh {@link Uni} for each subscriber.
     * <p>
     * Unlike {@link #item(Supplier)}, the supplier produces an {@link Uni} (and not an item).
     * <p>
     * If the supplier throws an exception, a failure event with the exception is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not produce {@code null}
     * @param <T> the type of item
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> deferred(Supplier<? extends Uni<? extends T>> supplier) {
        Supplier<? extends Uni<? extends T>> actual = ParameterValidation.nonNull(supplier, "supplier");
        return Infrastructure.onUniCreation(new UniCreateFromDeferredSupplier<>(actual));
    }

    /**
     * Creates a {@link Uni} that {@link Supplier#get supplies} an {@link Uni} to subscribe to for each
     * {@link UniSubscriber}. The supplier is called at subscription time.
     * <p>
     * In practice, it defers the {@link Uni} creation at subscription time and allows each subscriber to get different
     * {@link Uni}. So, it does not create the {@link Uni} until an {@link UniSubscriber subscriber} subscribes, and
     * creates a fresh {@link Uni} for each subscriber.
     * <p>
     * Unlike {@link #item(Supplier)}, the supplier produces an {@link Uni} (and not an item).
     * <p>
     * If the supplier throws an exception, a failure event with the exception is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     * <p>
     * This variant of {@link #deferred(Supplier)} allows passing a state supplier. This supplier allows
     * sharing some <em>state</em> between the subscribers. It is particularly useful when using {@link Uni#repeat()}
     * as you can pass a shared state (for example a page counter, like an AtomicInteger, if you implement pagination).
     * The state supplier is called once, during the first subscription. Note that the mapper is called for every
     * subscription.
     * <p>
     * The state supplier should produce a container wrapping the shared state. This shared state must be thread-safe.
     *
     * @param stateSupplier the state supplier, must not return {@code null}, must not be {@code null}
     * @param mapper the taking the shared state and producing the completion stage.
     * @param <T> the type of item
     * @param <S> the type of the state
     * @return the produced {@link Uni}
     */
    public <T, S> Uni<T> deferred(Supplier<S> stateSupplier, Function<S, ? extends Uni<? extends T>> mapper) {
        ParameterValidation.nonNull(stateSupplier, "stateSupplier");
        ParameterValidation.nonNull(mapper, "mapper");

        // Flag checking that the state supplier is only called once.
        AtomicBoolean once = new AtomicBoolean();
        // The shared state container.
        AtomicReference<S> state = new AtomicReference<>();

        return Uni.createFrom().deferred(() -> {
            try {
                invokeOnce(once, state, stateSupplier);
            } catch (Throwable e) {
                return Uni.createFrom().failure(e);
            }

            S sharedState = state.get();
            if (sharedState == null) {
                // The state supplier failed or produced null.
                return Uni.createFrom().failure(new IllegalStateException("Invalid shared state"));
            }
            return mapper.apply(sharedState);
        });
    }

    /**
     * Creates a {@link Uni} that emits a {@code failure} event immediately after being subscribed to.
     *
     * @param failure the failure to be fired, must not be {@code null}
     * @param <T> the virtual type of item used by the {@link Uni}, must be explicitly set as in
     *        {@code Uni.<String>failed(exception);}
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> failure(Throwable failure) {
        Throwable exception = ParameterValidation.nonNull(failure, "failure");
        return failure(() -> exception);
    }

    /**
     * Creates a {@link Uni} that emits a {@code failure} event produced using the passed supplier immediately after
     * being subscribed to. The supplier is called at subscription time, and produces an instance of {@link Throwable}.
     * If the supplier throws an exception, a {@code failure} event is fired with this exception.
     * If the supplier produces {@code null}, a {@code failure} event is fired with a {@link NullPointerException}.
     *
     * @param supplier the supplier producing the failure, must not be {@code null}, must not produce {@code null}
     * @param <T> the virtual type of item used by the {@link Uni}, must be explicitly set as in
     *        {@code Uni.<String>failed(exception);}
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> failure(Supplier<Throwable> supplier) {
        Supplier<Throwable> actual = ParameterValidation.nonNull(supplier, "supplier");

        return emitter(emitter -> {
            Throwable throwable;
            try {
                throwable = actual.get();
            } catch (Throwable e) {
                emitter.fail(e);
                return;
            }

            if (throwable == null) {
                emitter.fail(new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL));
            } else {
                emitter.fail(throwable);
            }
        });
    }

    /**
     * Creates a {@link Uni} that will never fire an {@code item} or {@code failure} event.
     *
     * @param <T> the virtual type of item
     * @return a never completing {@link Uni}
     */
    @SuppressWarnings("unchecked")
    public <T> Uni<T> nothing() {
        return (Uni<T>) UniNever.INSTANCE;
    }

    /**
     * Creates a {@link Uni} from the given {@link Multi}.
     * <p>
     * When a subscriber subscribes to the returned {@link Uni}, it subscribes to the {@link Multi} and requests one
     * item. The event emitted by the {@link Multi} are then forwarded to the {@link Uni}:
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
     * @param multi the multi, must not be {@code null}
     * @param <T> the type of item
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> multi(Multi<T> multi) {
        ParameterValidation.nonNull(multi, "multi");
        return multi.toUni();
    }
}
