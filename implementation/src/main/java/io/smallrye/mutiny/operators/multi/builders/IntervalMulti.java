package io.smallrye.mutiny.operators.multi.builders;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class IntervalMulti extends AbstractMulti<Long> {

    private final ScheduledExecutorService executor;
    private final Duration initialDelay;
    private final Duration period;

    public IntervalMulti(
            Duration initialDelay,
            Duration period,
            ScheduledExecutorService executor) {
        this.initialDelay = ParameterValidation.validate(initialDelay, "initialDelay");
        this.period = ParameterValidation.validate(period, "period");
        this.executor = ParameterValidation.nonNull(executor, "executor");
    }

    public IntervalMulti(
            Duration period,
            ScheduledExecutorService executor) {
        this.initialDelay = null;
        this.period = ParameterValidation.validate(period, "period");
        this.executor = ParameterValidation.nonNull(executor, "executor");
    }

    @Override
    public void subscribe(MultiSubscriber<? super Long> actual) {
        IntervalRunnable runnable = new IntervalRunnable(actual, period, initialDelay, executor);
        actual.onSubscribe(runnable);
        // Only start the ticks when we get the first request.
    }

    static final class IntervalRunnable implements Runnable, Flow.Subscription {
        private final MultiSubscriber<? super Long> actual;
        private final AtomicLong requested = new AtomicLong();
        private final Duration period;
        private final Duration initialDelay;
        private final ScheduledExecutorService executor;
        private volatile boolean cancelled;
        private volatile boolean once = true;

        private final AtomicLong count = new AtomicLong();
        private ScheduledFuture<?> future;

        IntervalRunnable(MultiSubscriber<? super Long> actual,
                Duration period, Duration initial, ScheduledExecutorService executor) {
            this.actual = actual;
            this.period = period;
            this.initialDelay = initial;
            this.executor = executor;
        }

        public void start() {
            try {
                synchronized (this) {
                    if (initialDelay != null) {
                        future = executor.scheduleAtFixedRate(this, initialDelay.toMillis(), period.toMillis(),
                                TimeUnit.MILLISECONDS);
                    } else {
                        future = executor.scheduleAtFixedRate(this, 0, period.toMillis(),
                                TimeUnit.MILLISECONDS);
                    }
                }
            } catch (RejectedExecutionException ree) {
                if (!cancelled) {
                    actual.onFailure(new RejectedExecutionException(ree));
                }
            }
        }

        @Override
        public void run() {
            if (!cancelled) {
                if (requested.get() != 0L) {
                    actual.onItem(count.getAndIncrement());
                    if (requested.get() != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }
                } else {
                    cancel();
                    actual.onFailure(
                            new BackPressureFailure("Could not emit tick " + count + " due to lack of requests"));
                }
            }
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
            }
            if (once) {
                start();
                once = false;
            }
        }

        @Override
        public synchronized void cancel() {
            cancelled = true;
            if (future != null) {
                future.cancel(false);
            }
        }

    }

}
