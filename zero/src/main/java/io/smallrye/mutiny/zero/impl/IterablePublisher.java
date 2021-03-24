package io.smallrye.mutiny.zero.impl;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class IterablePublisher<T> implements Publisher<T> {

    private final Iterable<T> iterable;

    public IterablePublisher(Iterable<T> iterable) {
        this.iterable = iterable;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        if (!iterable.iterator().hasNext()) {
            subscriber.onSubscribe(new AlreadyCompletedSubscription());
            subscriber.onComplete();
        } else {
            subscriber.onSubscribe(new CollectionSubscription(subscriber));
        }
    }

    private class CollectionSubscription implements Subscription {

        private final Subscriber<? super T> subscriber;

        private volatile boolean cancelled = false;
        private Iterator<T> iterator;
        private AtomicLong requested = new AtomicLong();

        CollectionSubscription(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
            iterator = iterable.iterator();
        }

        @Override
        public void request(long n) {
            if (n <= 0L) {
                cancel();
                subscriber.onError(Helper.negativeRequest(n));
                return;
            }
            if (Helper.add(requested, n) == 0) {
                if (n == Long.MAX_VALUE) {
                    deliverAll();
                } else {
                    deliver(n);
                }
            }
        }

        private void deliver(long n) {
            long emitted = 0;
            for (;;) {

                if (cancelled) {
                    return;
                }

                while (emitted != n) {
                    if (iterator.hasNext()) {
                        subscriber.onNext(iterator.next());
                        emitted++;
                    } else {
                        subscriber.onComplete();
                        return;
                    }
                }

                n = requested.get();
                if (n == emitted) {
                    n = requested.addAndGet(-emitted);
                    if (n == 0L) {
                        return;
                    }
                    emitted = 0L;
                }
            }
        }

        private void deliverAll() {
            while (iterator.hasNext()) {
                if (cancelled) {
                    return;
                }
                subscriber.onNext(iterator.next());
            }
            if (cancelled) {
                return;
            }
            cancelled = true;
            subscriber.onComplete();
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
