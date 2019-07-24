package io.smallrye.reactive.groups;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.adapt.UniAdaptFrom;
import io.smallrye.reactive.operators.*;
import io.smallrye.reactive.subscription.UniEmitter;
import io.smallrye.reactive.subscription.UniSubscriber;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.smallrye.reactive.helpers.ParameterValidation.*;


/**
 * Group methods allowing to create {@link Uni} instances from various sources.
 */
public class UniCreate {

    public static final UniCreate INSTANCE = new UniCreate();

    private UniCreate() {
        // avoid direct instantiation.
    }

    /**
     * Creates a {@link Uni} from the given {@link CompletionStage} or {@link CompletableFuture}.
     * The produced {@code Uni} emits the result of the passed  {@link CompletionStage}. If the {@link CompletionStage}
     * never completes (or failed), the produced {@link Uni} would not emit the {@code result} or {@code failure}
     * events.
     * <p>
     * Cancelling the subscription on the produced {@link Uni} cancels the passed {@link CompletionStage}
     * (calling {@link CompletableFuture#cancel(boolean)} on the future retrieved using
     * {@link CompletionStage#toCompletableFuture()}.
     * <p>
     * If the stage has already been completed (or failed), the produced {@link Uni} sends the result or failure
     * immediately after subscription. If it's not the case, the subscriber's callbacks are called on the thread used
     * by the passed {@link CompletionStage}.
     *
     * @param stage the stage, must not be {@code null}
     * @param <T>   the type of result
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> completionStage(CompletionStage<? extends T> stage) {
        CompletionStage<? extends T> actual = nonNull(stage, "stage");
        return completionStage(() -> actual);
    }

    /**
     * Creates a {@link Uni} from the given {@link CompletionStage} or {@link CompletableFuture}. The future is
     * created by invoking the passed {@link Supplier} <strong>lazily</strong> at subscription time.
     * <p>
     * The produced {@code Uni} emits the result of the passed  {@link CompletionStage}. If the {@link CompletionStage}
     * never completes (or failed), the produced {@link Uni} would not emit a result or a failure.
     * <p>
     * Cancelling the subscription on the produced {@link Uni} cancels the passed {@link CompletionStage}
     * (calling {@link CompletableFuture#cancel(boolean)} on the future retrieved using
     * {@link CompletionStage#toCompletableFuture()}.
     * <p>
     * If the produced stage has already been completed (or failed), the produced {@link Uni} sends the result or failure
     * immediately after subscription. If it's not the case the subscriber's callbacks are called on the thread used
     * by the passed {@link CompletionStage}.
     * <p>
     * If the supplier throws an exception, a failure event with the exception  is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not produce {@code null}
     * @param <T>      the type of result
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> completionStage(Supplier<? extends CompletionStage<? extends T>> supplier) {
        return new UniCreateFromCompletionStage<>(nonNull(supplier, "supplier"));
    }

    /**
     * Creates a {@link Uni} from the passed {@link Publisher}.
     * <p>
     * The produced {@link Uni} emits the <strong>first</strong> result/value emitted by the passed {@link Publisher}.
     * If the publisher emits multiple values, others are dropped. If the publisher emits a failure after a value, the
     * failure is dropped. If the publisher emits the completion signal before having emitted a value, the produced
     * {@link Uni} emits a {@code null} result event.
     * <p>
     * When a subscriber subscribes to the produced {@link Uni}, it subscribes to the {@link Publisher} and requests
     * {@code 1} result. When the first result is received, the subscription is cancelled. Note that each Uni's subscriber
     * would produce a new subscription.
     * <p>
     * If the Uni's observer cancels its subscription, the subscription to the {@link Publisher} is also cancelled.
     *
     * @param publisher the publisher, must not be {@code null}
     * @param <T>       the type of result
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> publisher(Publisher<? extends T> publisher) {
        Publisher<? extends T> actual = nonNull(publisher, "publisher");
        return new UniCreateFromPublisher<>(actual);
    }

    /**
     * Creates a new {@link Uni} that completes immediately after being subscribed to with the specified (potentially
     * {@code null}) value. The result is retrieved <strong>lazily</strong> at subscription time, using the passed
     * {@link Supplier}. Unlike {@link #deferred(Supplier)}, the supplier produces a result and not an {@link Uni}.
     * <p>
     * If the supplier produces {@code null}, {@code null} is used as result event.
     * If the supplier throws an exception, a failure event with the exception  is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the result supplier, must not be {@code null}, can produce {@code null}
     * @param <T> the type of result
     * @return the new {@link Uni}
     */
    public <T> Uni<T> result(Supplier<? extends T> supplier) {
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
            emitter.result(result);
        });
    }

    /**
     * Creates a new {@link Uni} that completes immediately after being subscribed to with the specified (potentially
     * {@code null}) result.
     *
     * @param result the result, can be {@code null}
     * @param <T> the type of result
     * @return the new {@link Uni}
     */
    public <T> Uni<T> result(T result) {
        return result(() -> result);
    }

    /**
     * Creates a new {@link Uni} that completes immediately after being subscribed to with the result based on the value
     * contained in the given optional if {@link Optional#isPresent()} or {@code null} otherwise.
     *
     * @param optional the optional, must not be {@code null}
     * @param <T>      the type of the produced result
     * @return the new {@link Uni}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public <T> Uni<T> optional(Optional<T> optional) {
        Optional<T> actual = nonNull(optional, "optional");
        return result(() -> actual.orElse(null));
    }

    /**
     * Creates a new {@link Uni} that completes immediately after being subscribed to with the result based on the value
     * contained in the given optional if {@link Optional#isPresent()} or {@code null} otherwise. Unlike
     * {@link #optional(Optional)}, the passed {@link Supplier} is called lazily at subscription time.
     * <p>
     * If the supplier throws an exception, a failure event with the exception  is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not return {@code null}
     * @param <T>      the type of the produced result
     * @return the new {@link Uni}
     */
    public <T> Uni<T> optional(Supplier<Optional<T>> supplier) {
        Supplier<Optional<T>> actual = nonNull(supplier, "supplier");
        return result(() -> actual.get().orElse(null));
    }


    /**
     * Creates a new {@link Uni} that emits a {@code null} result after the specified delay. The countdown starts
     * at subscription time.
     * <p>
     * Cancelling the subscription before the delay avoid the {@code result} event to be fired.
     *
     * @param duration the duration, must not be {@code null}
     * @param executor the executor emitting the result once the delay is passed, must not be {@code null}
     * @return the new {@link Uni}
     */
    Uni<Void> delay(Duration duration, ScheduledExecutorService executor) {
        Duration actual = validate(duration, "duration");
        ScheduledExecutorService actualExecutor = nonNull(executor, "executor");
        throw new UnsupportedOperationException("not implemented yet");
    }

    /**
     * Creates a new {@link Uni} that emits a {@code null} result after the specified delay. The countdown starts
     * at subscription time.
     * <p>
     * Cancelling the subscription before the delay avoid the {@code result} event to be fired.
     * <p>
     * Unlike {@link #delay(Duration, ScheduledExecutorService)}, this method use the default executor service to emit
     * the {@code result} event.
     *
     * @param duration the duration, must not be {@code null}
     * @return the new {@link Uni}
     */
    Uni<Void> delay(Duration duration) {
        Duration actual = validate(duration, "duration");
        throw new UnsupportedOperationException("not implemented yet");
    }

    /**
     * Creates a {@link Uni} deferring the logic to the given consumer. The consumer can be used with callback-based
     * APIs to fire at most one result (potentially {@code null}), or a failure event.
     * <p>
     * Using this method, you can produce a {@link Uni} based on listener or callbacks APIs. You register the listener
     * in the consumer and emits the result / failure events when the listener is invoked. Don't forget to unregister
     * the listener on cancellation.
     * <p>
     * Note that the emitter only forwards the first event, subsequent events are dropped.
     * <p>
     * If the consume throws an exception, a failure event with the exception is fired if the first event was already
     * fired.
     *
     * @param consumer callback receiving the {@link UniEmitter} and events downstream. The callback is
     *                 called for each subscriber (at subscription time). Must not be {@code null}
     * @param <T>      the type of result
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> emitter(Consumer<UniEmitter<? super T>> consumer) {
        Consumer<UniEmitter<? super T>> actual = nonNull(consumer, "consumer");
        return new UniCreateWithEmitter<>(actual);
    }

    /**
     * Creates a {@link Uni} that {@link Supplier#get supplies} an {@link Uni} to subscribe to for each
     * {@link UniSubscriber}. The supplier is called at subscription time.
     * <p>
     * In practice, it defers the {@link Uni} creation at subscription time and allows each subscriber to get different
     * {@link Uni}. So, it does not create the {@link Uni} until an {@link UniSubscriber subscriber} subscribes, and
     * creates a fresh {@link Uni} for each subscriber.
     * <p>
     * Unlike {@link #result(Supplier)}, the supplier produces an {@link Uni} (and not a result).
     * <p>
     * If the supplier throws an exception, a failure event with the exception  is fired. If the supplier produces
     * {@code null}, a failure event containing a {@link NullPointerException} is fired.
     *
     * @param supplier the supplier, must not be {@code null}, must not produce {@code null}
     * @param <T>      the type of result
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> deferred(Supplier<? extends Uni<? extends T>> supplier) {
        Supplier<? extends Uni<? extends T>> actual = nonNull(supplier, "supplier");
        return new UniCreateFromDeferredSupplier<>(actual);
    }

    /**
     * Creates a {@link Uni} that emits a {@code failure} event immediately after being subscribed to.
     *
     * @param failure the failure to be fired, must not be {@code null}
     * @param <T>     the virtual type of result used by the {@link Uni}, must be explicitly set as in
     *                {@code Uni.<String>failed(exception);}
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> failure(Throwable failure) {
        Throwable exception = nonNull(failure, "failure");
        return failure(() -> exception);
    }

    /**
     * Creates a {@link Uni} that emits a {@code failure} event produced using the passed supplier immediately after
     * being subscribed to. The supplier is called at subscription time, and produces an instance of {@link Throwable}.
     * If the supplier throws an exception, a {@code failure} event is fired with this exception.
     * If the supplier produces {@code null}, a {@code failure} event is fired with a {@link NullPointerException}.
     *
     * @param supplier the supplier producing the failure, must not be {@code null}, must not produce {@code null}
     * @param <T>      the virtual type of result used by the {@link Uni}, must be explicitly set as in
     *                 {@code Uni.<String>failed(exception);}
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> failure(Supplier<Throwable> supplier) {
        Supplier<Throwable> actual = nonNull(supplier, "supplier");

        return emitter(emitter -> {
            Throwable throwable;
            try {
                throwable = actual.get();
            } catch (Exception e) {
                emitter.failure(e);
                return;
            }

            if (throwable == null) {
                emitter.failure(new NullPointerException(SUPPLIER_PRODUCED_NULL));
            } else {
                emitter.failure(throwable);
            }
        });
    }

    /**
     * Creates a {@link Uni} that will never fire a {@code result} or {@code failure} event.
     *
     * @param <T> the virtual type of result
     * @return a never completing {@link Uni}
     */
    @SuppressWarnings("unchecked")
    public <T> Uni<T> nothing() {
        return (Uni<T>) UniNever.INSTANCE;
    }

    /**
     * Equivalent to {@link #result(Object)}} firing a {@code result} event with {@code null}.
     *
     * @return a {@link Uni} immediately calling {@link UniSubscriber#onResult(Object)} with {@code null} just
     * after subscription.
     */
    public Uni<Void> nullValue() {
        return result(() -> null);
    }


    public <T, X> Uni<T> converterOf(X instance) {
        return UniAdaptFrom.adaptFrom(instance);
    }
}
