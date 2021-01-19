package io.smallrye.mutiny.operators.uni;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniDelegatingSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnSubscribeCall<T> extends UniOperator<T, T> {

    private final Function<? super UniSubscription, Uni<?>> callback;

    public UniOnSubscribeCall(Uni<? extends T> upstream,
            Function<? super UniSubscription, Uni<?>> callback) {
        super(ParameterValidation.nonNull(upstream, "upstream"));
        this.callback = callback;
    }

    @Override
    protected void subscribing(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<T, T>(subscriber) {

            // As subscription might be delayed, we need to store the event provided by the upstream
            // until the uni provides a item or failure event. It would be illegal to forward these events before a
            // subscription.

            volatile T item;
            volatile Throwable failure;

            final AtomicBoolean done = new AtomicBoolean();

            @Override
            public void onSubscribe(UniSubscription subscription) {
                // Invoke producer
                Uni<?> uni;
                try {
                    uni = Objects.requireNonNull(callback.apply(subscription), "The produced Uni must not be `null`");
                } catch (Throwable e) {
                    // If the functions fails or returns null, propagates a failure.
                    super.onSubscribe(EmptyUniSubscription.CANCELLED);
                    super.onFailure(e);
                    return;
                }

                uni.subscribe().with(
                        ignored -> {
                            // Once the uni produces its item, propagates the subscription downstream
                            super.onSubscribe(subscription);
                            if (done.compareAndSet(false, true)) {
                                forwardPendingEvent();
                            }
                        },
                        failed -> {
                            // On failure, propagates the failure
                            super.onSubscribe(EmptyUniSubscription.CANCELLED);
                            super.onFailure(failed);
                        });
            }

            private void forwardPendingEvent() {
                if (item != null) {
                    super.onItem(item);
                } else if (failure != null) {
                    super.onFailure(failure);
                }
            }

            @Override
            public void onItem(T item) {
                if (done.get()) {
                    super.onItem(item);
                } else {
                    this.item = item;
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (done.get()) {
                    super.onFailure(failure);
                } else {
                    this.failure = failure;
                }
            }
        });
    }
}
