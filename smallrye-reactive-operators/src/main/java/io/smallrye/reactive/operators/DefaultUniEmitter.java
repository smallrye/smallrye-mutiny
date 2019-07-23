package io.smallrye.reactive.operators;


import io.smallrye.reactive.subscription.UniEmitter;
import io.smallrye.reactive.subscription.UniSubscriber;
import io.smallrye.reactive.subscription.UniSubscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;


/**
 * Implementation of the Uni Emitter.
 * This implementation makes sure:
 * <ul>
 * <li>only the first signal is propagated downstream</li>
 * <li>cancellation action is called only once and then drop</li>
 * </ul>
 * <p>
 *
 * @param <T> the type of result emitted by the emitter
 */
public class DefaultUniEmitter<T> implements UniEmitter<T>, UniSubscription {

    private final UniSubscriber<T> downstream;
    private final AtomicBoolean disposed = new AtomicBoolean();
    private final AtomicReference<Runnable> onCancellation = new AtomicReference<>();

    DefaultUniEmitter(UniSubscriber<T> subscriber) {
        this.downstream = nonNull(subscriber, "subscriber");
    }

    @Override
    public void result(T value) {
        if (disposed.compareAndSet(false, true)) {
            downstream.onResult(value);
        }
    }

    @Override
    public void failure(Throwable failure) {
        nonNull(failure, "failure");
        if (disposed.compareAndSet(false, true)) {
            downstream.onFailure(failure);
        }
    }

    @Override
    public UniEmitter<T> onCancellation(Runnable onCancel) {
        // Leak here, if the cancellation action is attached to late, it won't be released.
        onCancellation.set(nonNull(onCancel, "onCancel"));
        return this;
    }


    @Override
    public void cancel() {
        if (disposed.compareAndSet(false, true)) {
            Runnable runnable = onCancellation.getAndSet(null);
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    public boolean isCancelled() {
        return disposed.get();
    }
}
