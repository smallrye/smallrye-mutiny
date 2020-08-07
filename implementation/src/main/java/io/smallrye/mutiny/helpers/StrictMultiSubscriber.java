package io.smallrye.mutiny.helpers;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Ensures that the events between the upstream and downstream follow
 * the Reactive Streams specification. Typically:
 * <ul>
 * <li>1.3: onNext should not be called concurrently until onSubscribe returns</li>
 * <li>2.3: onError or onComplete must not call cancel</li>
 * <li>3.9: negative requests should emit an onError(IllegalArgumentException)</li>
 * <li>2.12: onSubscribe must be called at most once (subscription cancelled and onError called)</li>
 * </ul>
 * 
 * @param <T> the type of item
 */
public class StrictMultiSubscriber<T>
        implements MultiSubscriber<T>, Subscription {

    private final AtomicInteger wip = new AtomicInteger();

    private final Subscriber<? super T> downstream;

    private final AtomicReference<Throwable> failure;

    private final AtomicLong requested;

    private final AtomicReference<Subscription> upstream;

    private final AtomicBoolean once;

    volatile boolean done;

    public StrictMultiSubscriber(Subscriber<? super T> downstream) {
        this.downstream = downstream;
        this.failure = new AtomicReference<>();
        this.requested = new AtomicLong();
        this.upstream = new AtomicReference<>();
        this.once = new AtomicBoolean();
    }

    @Override
    public void request(long n) {
        if (n <= 0) {
            cancel();
            onError(new IllegalArgumentException("Reactive Streams Rule 3.9 violated: request must be positive, but was " + n));
        } else {
            Subscriptions.requestIfNotNullOrAccumulate(upstream, requested, n);
        }
    }

    @Override
    public void cancel() {
        if (!done) {
            Subscriptions.cancel(upstream);
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (once.compareAndSet(false, true)) {
            downstream.onSubscribe(this);
            Subscriptions.setIfEmptyAndRequest(this.upstream, requested, s);
        } else {
            s.cancel();
            cancel();
            onError(new IllegalStateException("Reactive Streams Rule 2.12 violated: onSubscribe must be called at most once"));
        }
    }

    @Override
    public void onItem(T t) {
        Objects.requireNonNull(t);
        HalfSerializer.onNext(downstream, t, wip, failure);
    }

    @Override
    public void onFailure(Throwable t) {
        done = true;
        HalfSerializer.onError(downstream, t, wip, failure);
    }

    @Override
    public void onCompletion() {
        done = true;
        HalfSerializer.onComplete(downstream, wip, failure);
    }
}
