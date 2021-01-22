package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniDelegatingSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

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
        AtomicReference<ScheduledFuture<?>> holder = new AtomicReference<>();
        AtomicReference<UniSubscription> reference = new AtomicReference<>();
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                if (reference.compareAndSet(null, subscription)) {
                    super.onSubscribe(() -> {
                        if (reference.compareAndSet(subscription, CANCELLED)) {
                            subscription.cancel();
                            ScheduledFuture<?> future = holder.getAndSet(null);
                            if (future != null) {
                                future.cancel(true);
                            }
                        }
                    });
                }
            }

            @Override
            public void onItem(T item) {
                if (reference.get() != CANCELLED) {
                    try {
                        ScheduledFuture<?> future = executor
                                .schedule(() -> super.onItem(item), duration.toMillis(), TimeUnit.MILLISECONDS);
                        holder.set(future);
                    } catch (RuntimeException e) {
                        // Typically, a rejected execution exception
                        super.onFailure(e);
                    }
                }
            }
        });
    }
}
