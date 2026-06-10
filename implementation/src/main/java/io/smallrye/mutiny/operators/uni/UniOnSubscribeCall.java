package io.smallrye.mutiny.operators.uni;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
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
    public void subscribe(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniOnSubscribeCallProcessor(subscriber));
    }

    private class UniOnSubscribeCallProcessor extends UniOperatorProcessor<T, T> {

        private static final Object NO_ITEM = new Object();

        public UniOnSubscribeCallProcessor(UniSubscriber<? super T> downstream) {
            super(downstream);
        }

        private volatile Object item = NO_ITEM;
        private volatile Throwable failure;

        private final AtomicBoolean done = new AtomicBoolean();

        @Override
        public void onSubscribe(UniSubscription subscription) {
            Uni<?> uni;
            try {
                uni = Objects.requireNonNull(callback.apply(subscription), "The produced Uni must not be `null`");
            } catch (Throwable e) {
                downstream.onSubscribe(EmptyUniSubscription.DONE);
                downstream.onFailure(e);
                return;
            }

            uni.subscribe().with(
                    context(),
                    ignored -> {
                        downstream.onSubscribe(subscription);
                        if (done.compareAndSet(false, true)) {
                            forwardPendingEvent();
                        }
                    },
                    failed -> {
                        done.set(true);
                        subscription.cancel();
                        downstream.onSubscribe(EmptyUniSubscription.DONE);
                        downstream.onFailure(failed);
                    });
        }

        @SuppressWarnings("unchecked")
        private void forwardPendingEvent() {
            if (item != NO_ITEM) {
                downstream.onItem((T) item);
            } else if (failure != null) {
                downstream.onFailure(failure);
            }
        }

        @Override
        public void onItem(T item) {
            if (done.get()) {
                downstream.onItem(item);
            } else {
                this.item = item;
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (done.get()) {
                downstream.onFailure(failure);
            } else {
                this.failure = failure;
            }
        }
    }
}
