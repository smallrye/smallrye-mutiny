package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.helpers.ParameterValidation;
import io.smallrye.reactive.unimulti.infrastructure.Infrastructure;
import io.smallrye.reactive.unimulti.subscription.UniSubscriber;
import io.smallrye.reactive.unimulti.subscription.UniSubscription;

public class UniFailOnTimeout<I> extends UniOperator<I, I> {
    private final Duration timeout;
    private final Supplier<Throwable> supplier;
    private final ScheduledExecutorService executor;

    public UniFailOnTimeout(Uni<I> upstream, Duration timeout, Supplier<Throwable> supplier,
            ScheduledExecutorService executor) {
        super(nonNull(upstream, "upstream"));
        this.timeout = validate(timeout, "onTimeout");
        this.supplier = nonNull(supplier, "supplier");
        this.executor = executor == null ? Infrastructure.getDefaultWorkerPool() : executor;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        AtomicBoolean doneOrCancelled = new AtomicBoolean();
        AtomicReference<ScheduledFuture<?>> task = new AtomicReference<>();

        upstream().subscribe().withSubscriber(new UniSubscriber<I>() {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                // Configure the watch dog at subscription time.
                try {
                    task.set(executor.schedule(() -> {
                        if (doneOrCancelled.compareAndSet(false, true)) {
                            sendTimeout(subscriber, subscription);
                        }
                    }, timeout.toMillis(), TimeUnit.MILLISECONDS));
                } catch (RejectedExecutionException e) {
                    // Executor out of service.
                    subscriber.onFailure(e);
                    return;
                }

                subscriber.onSubscribe(() -> {
                    if (doneOrCancelled.compareAndSet(false, true)) {
                        // cancelling
                        subscription.cancel();
                        ScheduledFuture<?> future = task.getAndSet(null);
                        if (future != null) {
                            future.cancel(false);
                        }
                    }
                });
            }

            @Override
            public void onItem(I item) {
                if (doneOrCancelled.compareAndSet(false, true)) {
                    ScheduledFuture<?> future = task.getAndSet(null);
                    if (future != null) {
                        future.cancel(false);
                    }
                    subscriber.onItem(item);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (doneOrCancelled.compareAndSet(false, true)) {
                    ScheduledFuture<?> future = task.getAndSet(null);
                    if (future != null) {
                        future.cancel(false);
                    }
                    subscriber.onFailure(failure);
                }
            }
        });
    }

    private void sendTimeout(UniSerializedSubscriber<? super I> subscriber, UniSubscription subscription) {

        // Cancel the upstream subscription
        subscription.cancel();

        Throwable throwable;
        try {
            throwable = supplier.get();
        } catch (Exception e) {
            subscriber.onFailure(e);
            return;
        }
        if (throwable == null) {
            subscriber.onFailure(new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL));
        } else {
            subscriber.onFailure(throwable);
        }
    }
}
