package io.smallrye.reactive.operators.multi.overflow;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.operators.multi.AbstractMultiWithUpstream;
import io.smallrye.reactive.operators.multi.MultiOperatorSubscriber;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class MultiOnOverflowDropItemsOp<T> extends AbstractMultiWithUpstream<T, T> {

    private final Consumer<? super T> onItemDrop;

    public MultiOnOverflowDropItemsOp(Multi<T> upstream) {
        super(upstream);
        this.onItemDrop = null;
    }

    public MultiOnOverflowDropItemsOp(Multi<T> upstream, Consumer<? super T> onItemDrop) {
        super(upstream);
        this.onItemDrop = onItemDrop;
    }

    public void subscribe(Subscriber<? super T> downstream) {
        upstream.subscribe(new MultiOnOverflowDropItemsSubscriber<T>(downstream, onItemDrop));
    }

    static final class MultiOnOverflowDropItemsSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        private final Consumer<? super T> onItemDrop;
        private final AtomicLong requested = new AtomicLong();

        MultiOnOverflowDropItemsSubscriber(Subscriber<? super T> dowsntream, Consumer<? super T> onItemDrop) {
            super(dowsntream);
            this.onItemDrop = onItemDrop;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                downstream.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onNext(T item) {
            if (isDone()) {
                return;
            }
            long req = requested.get();
            if (req != 0L) {
                downstream.onNext(item);
                Subscriptions.substract(requested, 1);
            } else {
                // no request, dropping.
                drop(item);
            }
        }

        private void drop(T item) {
            if (onItemDrop != null) {
                try {
                    onItemDrop.accept(item);
                } catch (Throwable e) {
                    onError(e);
                }
            }
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
            }
        }
    }
}