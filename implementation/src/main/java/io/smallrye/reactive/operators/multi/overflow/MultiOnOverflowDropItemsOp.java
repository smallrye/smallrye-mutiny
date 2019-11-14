package io.smallrye.reactive.operators.multi.overflow;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.operators.multi.AbstractMultiOperator;
import io.smallrye.reactive.operators.multi.MultiOperatorProcessor;

public class MultiOnOverflowDropItemsOp<T> extends AbstractMultiOperator<T, T> {

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
        upstream.subscribe(new MultiOnOverflowDropItemsProcessor<T>(downstream, onItemDrop));
    }

    static final class MultiOnOverflowDropItemsProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final Consumer<? super T> onItemDrop;
        private final AtomicLong requested = new AtomicLong();

        MultiOnOverflowDropItemsProcessor(Subscriber<? super T> dowsntream, Consumer<? super T> onItemDrop) {
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
                Subscriptions.subtract(requested, 1);
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
