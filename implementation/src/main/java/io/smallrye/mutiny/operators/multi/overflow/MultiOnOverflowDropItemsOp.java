package io.smallrye.mutiny.operators.multi.overflow;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnOverflowDropItemsOp<T> extends AbstractMultiOperator<T, T> {

    private final Consumer<T> dropConsumer;
    private final Function<T, Uni<?>> dropUniMapper;

    public MultiOnOverflowDropItemsOp(Multi<? extends T> upstream, Consumer<T> dropConsumer,
            Function<T, Uni<?>> dropUniMapper) {
        super(upstream);
        this.dropConsumer = dropConsumer;
        this.dropUniMapper = dropUniMapper;
    }

    public MultiOnOverflowDropItemsOp(Multi<T> upstream) {
        this(upstream, null, null);
    }

    public MultiOnOverflowDropItemsOp(Multi<T> upstream, Consumer<T> dropConsumer) {
        this(upstream, dropConsumer, null);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiOnOverflowDropItemsProcessor(downstream));
    }

    class MultiOnOverflowDropItemsProcessor extends MultiOperatorProcessor<T, T> {

        private final AtomicLong requested = new AtomicLong();

        MultiOnOverflowDropItemsProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (compareAndSetUpstreamSubscription(null, subscription)) {
                downstream.onSubscribe(this);
                getUpstreamSubscription().request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onItem(T item) {
            if (isDone()) {
                return;
            }
            long req = requested.get();
            if (req != 0L) {
                downstream.onItem(item);
                Subscriptions.subtract(requested, 1);
            } else {
                // no request, dropping.
                if (dropConsumer != null) {
                    notifyOnOverflowInvoke(item);
                } else if (dropUniMapper != null) {
                    notifyOnOverflowCall(item);
                }
            }
        }

        private void notifyOnOverflowInvoke(T item) {
            try {
                dropConsumer.accept(item);
            } catch (Throwable e) {
                super.onFailure(e);
            }
        }

        private void notifyOnOverflowCall(T item) {
            try {
                Uni<?> uni = nonNull(dropUniMapper.apply(item), "uni");
                uni.subscribe().with(
                        ignored -> {
                            // Just drop and ignore
                        },
                        super::onFailure);
            } catch (Throwable failure) {
                super.onFailure(failure);
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
