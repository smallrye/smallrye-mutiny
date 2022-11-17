package io.smallrye.mutiny.helpers.test;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A convenient base class for a subscriber and subscription to extend in tests and that manages the subscription and
 * requests.
 * <p>
 *
 * Implementations shall override {@link AbstractSubscriber#onNext(Object)},
 * {@link AbstractSubscriber#onError(Throwable)} and/or {@link AbstractSubscriber#onComplete()} to add test-specific
 * custom logic.
 *
 * @param <T> the type of the items
 */
@SuppressWarnings({ "SubscriberImplementation" })
public class AbstractSubscriber<T> implements Subscriber<T>, Subscription {

    private final long upfrontRequest;

    /**
     * Creates a new {@link AbstractSubscriber} with 0 upfront requests.
     */
    public AbstractSubscriber() {
        upfrontRequest = 0;
    }

    /**
     * Creates a new {@link AbstractSubscriber} with {@code req} upfront requests.
     *
     * @param req the number of upfront requests
     */
    public AbstractSubscriber(long req) {
        upfrontRequest = req;
    }

    private final AtomicReference<Subscription> upstream = new AtomicReference<>();

    @Override
    public void onSubscribe(Subscription s) {
        if (upstream.compareAndSet(null, s)) {
            if (upfrontRequest > 0) {
                s.request(upfrontRequest);
            }
        } else {
            throw new IllegalStateException("We already have a subscription");
        }
    }

    @Override
    public void onNext(T t) {

    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public void request(long n) {
        Subscription subscription = upstream.get();
        if (subscription != null) {
            subscription.request(n);
        } else {
            throw new IllegalStateException("No subscription");
        }
    }

    @Override
    public void cancel() {
        Subscription subscription = upstream.get();
        if (subscription != null) {
            subscription.cancel();
        } else {
            throw new IllegalStateException("No subscription");
        }
    }
}
