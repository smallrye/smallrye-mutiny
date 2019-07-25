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
 * <li>only the first event is propagated downstream</li>
 * <li>termination action is called only once and then drop</li>
 * </ul>
 * <p>
 *
 * @param <T> the type of result emitted by the emitter
 */
public class DefaultUniEmitter<T> implements UniEmitter<T>, UniSubscription {

    private final UniSubscriber<T> downstream;
    private final AtomicBoolean disposed = new AtomicBoolean();
    private final AtomicReference<Runnable> onTermination = new AtomicReference<>();

    DefaultUniEmitter(UniSubscriber<T> subscriber) {
        this.downstream = nonNull(subscriber, "subscriber");
    }

    @Override
    public void result(T value) {
        if (disposed.compareAndSet(false, true)) {
            downstream.onResult(value);
            terminate();
        }
    }

    private void terminate() {
        Runnable runnable = onTermination.getAndSet(null);
        if (runnable != null) {
            runnable.run();
        }
    }

    @Override
    public void failure(Throwable failure) {
        nonNull(failure, "failure");
        if (disposed.compareAndSet(false, true)) {
            downstream.onFailure(failure);
            terminate();
        }
    }

    @Override
    public UniEmitter<T> onTermination(Runnable callback) {
        Runnable actual = nonNull(callback, "callback");
        if(! disposed.get()) {
            this.onTermination.set(actual);
            // Re-check if the termination didn't happen in the meantime
            if (disposed.get()) {
                terminate();
            }
        }
        return this;
    }


    @Override
    public void cancel() {
        if (disposed.compareAndSet(false, true)) {
            terminate();
        }
    }

    public boolean isTerminated() {
        return disposed.get();
    }
}
