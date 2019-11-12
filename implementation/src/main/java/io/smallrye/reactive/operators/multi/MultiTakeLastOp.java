package io.smallrye.reactive.operators.multi;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of multi, caching and emitting the last n items from upstream (emitted before the upstream completion).
 *
 * @param <T> the type of item
 */
public class MultiTakeLastOp<T> extends AbstractMultiWithUpstream<T, T> {

    private final int numberOfItems;

    public MultiTakeLastOp(Multi<? extends T> upstream, int numberOfItems) {
        super(upstream);
        this.numberOfItems = ParameterValidation.positiveOrZero(numberOfItems, "numberOfItems");
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        if (numberOfItems == 0) {
            upstream.subscribe(new TakeLastZeroSubscriber<>(actual));
        } else {
            upstream.subscribe(new TakeLastManySubscriber<>(actual, numberOfItems));
        }
    }

    static final class TakeLastZeroSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        TakeLastZeroSubscriber(Subscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                // Propagate subscription to downstream.
                downstream.onSubscribe(this);
                // Dropping all values.
                subscription.request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onNext(T t) {
            // Do nothing, we are dropping all the values.
        }
    }

    static final class TakeLastManySubscriber<T> extends MultiOperatorSubscriber<T, T> {

        private final int numberOfItems;
        private final ArrayDeque<T> queue;
        private final AtomicLong requested = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();
        volatile boolean upstreamCompleted;

        TakeLastManySubscriber(Subscriber<? super T> downstream, int numberOfItems) {
            super(downstream);
            this.numberOfItems = numberOfItems;
            this.queue = new ArrayDeque<>(numberOfItems);
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
                drain();
            }
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                // Propagate subscription to downstream.
                downstream.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);

            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onNext(T t) {
            if (queue.size() == numberOfItems) {
                queue.poll();
            }
            queue.offer(t);
        }

        @Override
        public void onComplete() {
            upstreamCompleted = true;
            drain();
        }

        private void drain() {
            if (wip.getAndIncrement() == 0) {
                long req = requested.get();
                do {
                    if (upstream.get() == Subscriptions.CANCELLED) {
                        return;
                    }
                    if (upstreamCompleted) {
                        long count = 0L;

                        while (count != req) {
                            if (upstream.get() == Subscriptions.CANCELLED) {
                                return;
                            }
                            T item = queue.poll();
                            if (item == null) {
                                // No more items in the queue, completing.
                                downstream.onComplete();
                                return;
                            }

                            downstream.onNext(item);
                            count++;
                        }

                        if (count != 0L && req != Long.MAX_VALUE) {
                            req = requested.addAndGet(-count);
                        }
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }

    }
}
