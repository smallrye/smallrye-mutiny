package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniDelayOnItem<T> extends UniOperator<T, T> {
    private final Duration duration;
    private final ScheduledExecutorService executor;

    public UniDelayOnItem(Uni<T> upstream, Duration duration, ScheduledExecutorService executor) {
        super(nonNull(upstream, "upstream"));
        this.duration = validate(duration, "duration");
        this.executor = nonNull(executor, "executor");
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelayOnItemProcessor(subscriber));
    }

    private class UniDelayOnItemProcessor extends UniOperatorProcessor<T, T> {

        private volatile ScheduledFuture<?> scheduledFuture;

        public UniDelayOnItemProcessor(UniSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void cancel() {
            if (!isCancelled()) {
                super.cancel();
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(true);
                }
            }
        }

        @Override
        public void onItem(T item) {
            if (!isCancelled()) {
                try {
                    Runnable dispatch = () -> downstream.onItem(item);
                    scheduledFuture = executor.schedule(dispatch, duration.toMillis(), TimeUnit.MILLISECONDS);
                } catch (Throwable err) {
                    downstream.onFailure(err);
                }
            }
        }
    }
}
