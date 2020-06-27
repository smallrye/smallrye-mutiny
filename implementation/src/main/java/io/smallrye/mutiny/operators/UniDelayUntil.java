package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniDelayUntil<T> extends UniOperator<T, T> {
    private final Function<? super T, ? extends Uni<?>> function;
    private final ScheduledExecutorService executor;

    public UniDelayUntil(Uni<T> upstream, Function<? super T, ? extends Uni<?>> function,
            ScheduledExecutorService executor) {
        super(nonNull(upstream, "upstream"));
        this.function = nonNull(function, "function");
        this.executor = nonNull(executor, "executor");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        AtomicReference<UniSubscription> reference = new AtomicReference<>();
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                if (reference.compareAndSet(null, subscription)) {
                    super.onSubscribe(() -> {
                        if (reference.compareAndSet(subscription, CANCELLED)) {
                            subscription.cancel();
                        }
                    });
                }
            }

            @Override
            public void onItem(T item) {
                if (reference.get() != CANCELLED) {
                    try {
                        Uni<?> uni = function.apply(item);
                        if (uni == null) {
                            super.onFailure(
                                    new NullPointerException("The function returned `null` instead of a valid `Uni`"));
                            return;
                        }
                        uni
                                .runSubscriptionOn(executor)
                                .subscribe().with(
                                        x -> {
                                            if (reference.get() != CANCELLED) {
                                                super.onItem(item);
                                            }
                                        },
                                        super::onFailure);
                    } catch (RuntimeException e) {
                        super.onFailure(e);
                    }
                }
            }
        });
    }
}
