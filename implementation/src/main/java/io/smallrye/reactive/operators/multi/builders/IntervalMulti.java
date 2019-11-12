package io.smallrye.reactive.operators.multi.builders;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.operators.AbstractMulti;
import io.smallrye.reactive.subscription.BackPressureFailure;

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
    protected Publisher<Long> publisher() {
        return this;
    }

    @Override
    public void subscribe(Subscriber<? super Long> actual) {
        IntervalRunnable runnable = new IntervalRunnable(actual);

        actual.onSubscribe(runnable);

        try {
            if (initialDelay != null) {
                executor.scheduleAtFixedRate(runnable, initialDelay.toMillis(), period.toMillis(),
                        TimeUnit.MILLISECONDS);
            } else {
                executor.scheduleAtFixedRate(runnable, 0, period.toMillis(),
                        TimeUnit.MILLISECONDS);
            }
        } catch (RejectedExecutionException ree) {
            if (!runnable.cancelled.get()) {
                actual.onError(new RejectedExecutionException(ree));
            }
        }
    }

    static final class IntervalRunnable implements Runnable, Subscription {
        private final Subscriber<? super Long> actual;
        private final AtomicLong requested = new AtomicLong();
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private final AtomicLong count = new AtomicLong();

        IntervalRunnable(Subscriber<? super Long> actual) {
            this.actual = actual;
        }

        @Override
        public void run() {
            if (!cancelled.get()) {
                if (requested.get() != 0L) {
                    actual.onNext(count.getAndIncrement());
                    if (requested.get() != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }
                } else {
                    cancel();
                    actual.onError(
                            new BackPressureFailure("Could not emit tick " + count + " due to lack of requests"));
                }
            }
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
            }
        }

        @Override
        public void cancel() {
            cancelled.set(true);
        }
    }
}
