package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.time.Duration;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.MultiOperator;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiFailOnItemTimeout<I> extends MultiOperator<I, I> {
    private final Duration timeout;
    private final Supplier<? extends Throwable> supplier;
    private final ScheduledExecutorService executor;

    public MultiFailOnItemTimeout(Multi<I> upstream, Duration timeout, Supplier<? extends Throwable> supplier,
            ScheduledExecutorService executor) {
        super(upstream);
        this.timeout = timeout;
        this.supplier = supplier;
        this.executor = executor == null ? Infrastructure.getDefaultWorkerPool() : executor;
    }

    @Override
    public void subscribe(MultiSubscriber<? super I> subscriber) {
        upstream().subscribe().withSubscriber(new MultiFailOnItemTimeoutProcessor(subscriber));
    }

    public class MultiFailOnItemTimeoutProcessor extends MultiOperatorProcessor<I, I> {

        private volatile ScheduledFuture<?> timeoutFuture;

        public MultiFailOnItemTimeoutProcessor(MultiSubscriber<? super I> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (!scheduleTimeout()) {
                subscription.cancel();
                downstream.onSubscribe(EmptyUniSubscription.DONE);
            }
            super.onSubscribe(subscription);
        }

        @Override
        public void onItem(I item) {
            Subscription sub = getUpstreamSubscription();
            if (sub != CANCELLED) {
                if (timeoutFuture != null) {
                    timeoutFuture.cancel(false);
                    if (!scheduleTimeout()) {
                        //On failure is already called in scheduleTimeout
                        return;
                    }
                }
                downstream.onItem(item);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            Subscription sub = getAndSetUpstreamSubscription(CANCELLED);
            if (sub != CANCELLED) {
                if (timeoutFuture != null) {
                    timeoutFuture.cancel(false);
                }
                downstream.onFailure(failure);
            } else {
                Infrastructure.handleDroppedException(failure);
            }
        }

        @Override
        public void onCompletion() {
            Subscription sub = getAndSetUpstreamSubscription(CANCELLED);
            if (sub != CANCELLED) {
                if (timeoutFuture != null) {
                    timeoutFuture.cancel(false);
                }
                downstream.onCompletion();
            }
        }

        @Override
        public void cancel() {
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
            }
            super.cancel();
        }

        private void doTimeout() {
            if (isCancelled()) {
                return;
            }
            super.cancel();

            Throwable throwable;
            try {
                throwable = supplier.get();
            } catch (Throwable e) {
                downstream.onFailure(e);
                return;
            }
            if (throwable == null) {
                downstream.onFailure(new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL));
            } else {
                downstream.onFailure(throwable);
            }
        }

        private boolean scheduleTimeout() {
            try {
                timeoutFuture = executor.schedule(this::doTimeout, timeout.toMillis(), TimeUnit.MILLISECONDS);
                return true;
            } catch (RejectedExecutionException e) {
                // Executor out of service.
                getAndSetUpstreamSubscription(CANCELLED);
                downstream.onFailure(e);
                return false;
            }
        }
    }
}
