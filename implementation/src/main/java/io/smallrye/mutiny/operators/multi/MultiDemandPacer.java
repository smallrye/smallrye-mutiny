package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.DemandPacer;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiDemandPacer<T> extends AbstractMultiOperator<T, T> {

    private final ScheduledExecutorService executor;
    private final DemandPacer pacer;

    public MultiDemandPacer(Multi<? extends T> upstream, ScheduledExecutorService executor, DemandPacer pacer) {
        super(upstream);
        this.executor = executor;
        this.pacer = pacer;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        upstream.subscribe(new MultiSubscriptionPacerProcessor<>(subscriber, executor, pacer));
    }

    private static class MultiSubscriptionPacerProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final ScheduledExecutorService executor;
        private final DemandPacer pacer;

        private final AtomicLong itemsCounter = new AtomicLong();
        private ScheduledFuture<?> scheduledFuture;
        private DemandPacer.Request currentRequest;

        MultiSubscriptionPacerProcessor(MultiSubscriber<? super T> downstream, ScheduledExecutorService executor,
                DemandPacer pacer) {
            super(downstream);
            this.executor = executor;
            this.pacer = pacer;
        }

        @Override
        public Context context() {
            if (downstream instanceof ContextSupport) {
                return ((ContextSupport) downstream).context();
            } else {
                return Context.empty();
            }
        }

        private void demandAndSchedule(ScheduledExecutorService executor) {
            if (upstream == Subscriptions.CANCELLED) {
                return;
            }
            long demand = currentRequest.demand();
            long delay = currentRequest.delay().toNanos();
            scheduledFuture = executor.schedule(this::tick, delay, TimeUnit.NANOSECONDS);
            upstream.request(demand);
        }

        private void tick() {
            if (upstream == Subscriptions.CANCELLED) {
                return;
            }
            long numberOfItemsEmitted = itemsCounter.getAndSet(0L);
            try {
                currentRequest = pacer.apply(currentRequest, numberOfItemsEmitted);
                if (currentRequest == null) {
                    cancel();
                    downstream.onFailure(new NullPointerException("The pacer provided a null request"));
                    return;
                }
            } catch (Throwable failure) {
                cancel();
                downstream.onFailure(failure);
                return;
            }
            demandAndSchedule(executor);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (compareAndSetUpstreamSubscription(null, subscription)) {
                downstream.onSubscribe(this);
                try {
                    currentRequest = pacer.initial();
                } catch (Throwable failure) {
                    cancel();
                    downstream.onFailure(failure);
                    return;
                }
                if (currentRequest == null) {
                    cancel();
                    downstream.onFailure(new NullPointerException("The pacer provided a null initial request"));
                } else {
                    demandAndSchedule(executor);
                }
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onItem(T item) {
            if (upstream == Subscriptions.CANCELLED) {
                return;
            }
            itemsCounter.incrementAndGet();
            downstream.onItem(item);
        }

        @Override
        public void onFailure(Throwable failure) {
            if (upstream == Subscriptions.CANCELLED) {
                return;
            }
            cancel();
            downstream.onFailure(failure);
        }

        @Override
        public void onCompletion() {
            if (upstream == Subscriptions.CANCELLED) {
                return;
            }
            cancel();
            downstream.onCompletion();
        }

        @Override
        public void cancel() {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }
            super.cancel();
        }

        @Override
        public void request(long numberOfItems) {
            // Ignore on purpose
        }
    }
}
