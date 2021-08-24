package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniFailOnTimeout<I> extends UniOperator<I, I> {
    private final Duration timeout;
    private final Supplier<? extends Throwable> supplier;
    private final ScheduledExecutorService executor;

    public UniFailOnTimeout(Uni<I> upstream, Duration timeout, Supplier<? extends Throwable> supplier,
            ScheduledExecutorService executor) {
        super(upstream);
        this.timeout = timeout;
        this.supplier = supplier;
        this.executor = executor == null ? Infrastructure.getDefaultWorkerPool() : executor;
    }

    @Override
    public void subscribe(UniSubscriber<? super I> subscriber) {
        AbstractUni.subscribe(upstream(), new UniFailOnTimeoutProcessor(subscriber));
    }

    private class UniFailOnTimeoutProcessor extends UniOperatorProcessor<I, I> {

        private volatile ScheduledFuture<?> timeoutFuture;

        public UniFailOnTimeoutProcessor(UniSubscriber<? super I> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(UniSubscription subscription) {
            try {
                timeoutFuture = executor.schedule(this::doTimeout, timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException e) {
                // Executor out of service.
                getAndSetUpstreamSubscription(CANCELLED);
                subscription.cancel();
                downstream.onSubscribe(EmptyUniSubscription.DONE);
                downstream.onFailure(e);
                return;
            }
            super.onSubscribe(subscription);
        }

        @Override
        public void onItem(I item) {
            UniSubscription sub = getAndSetUpstreamSubscription(CANCELLED);
            if (sub != CANCELLED) {
                if (timeoutFuture != null) {
                    timeoutFuture.cancel(false);
                }
                downstream.onItem(item);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            UniSubscription sub = getAndSetUpstreamSubscription(CANCELLED);
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
    }
}
