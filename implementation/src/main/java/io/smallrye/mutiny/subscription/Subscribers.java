package io.smallrye.mutiny.subscription;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class Subscribers {

    @SuppressWarnings("ThrowableNotThrown")
    public static final Consumer<? super Throwable> NO_ON_FAILURE = failure -> new Exception(
            "Missing onFailure/onError handler in the subscriber", failure).printStackTrace(); // NOSONAR

    public static <T> CancellableSubscriber<T> from(Consumer<? super T> onItem, Consumer<? super Throwable> onFailure,
            Runnable onCompletion,
            Consumer<? super Subscription> onSubscription) {
        return new CallbackBasedSubscriber<>(onItem, onFailure, onCompletion, onSubscription);
    }

    public static class CallbackBasedSubscriber<T> implements CancellableSubscriber<T>, Subscription {

        private final AtomicReference<Subscription> subscription = new AtomicReference<>();
        private final Consumer<? super T> onItem;
        private final Consumer<? super Throwable> onFailure;
        private final Runnable onCompletion;
        private final Consumer<? super Subscription> onSubscription;

        public CallbackBasedSubscriber(
                Consumer<? super T> onItem,
                Consumer<? super Throwable> onFailure,
                Runnable onCompletion,
                Consumer<? super Subscription> onSubscription) {
            this.onItem = nonNull(onItem, "onItem");
            this.onFailure = onFailure;
            this.onCompletion = onCompletion;
            this.onSubscription = nonNull(onSubscription, "onSubscription");
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (subscription.compareAndSet(null, s)) {
                try {
                    // onSubscription cannot be null
                    onSubscription.accept(this);
                } catch (Throwable ex) {
                    s.cancel();
                    onError(ex);
                }
            } else {
                s.cancel();
            }
        }

        @Override
        public void onItem(T item) {
            Objects.requireNonNull(item);
            if (subscription.get() != Subscriptions.CANCELLED) {
                try {
                    // onItem cannot be null.
                    onItem.accept(item);
                } catch (Throwable e) {
                    subscription.getAndSet(Subscriptions.CANCELLED).cancel();
                    onError(e);
                }
            }
        }

        @Override
        public void onFailure(Throwable t) {
            Objects.requireNonNull(t);
            if (subscription.getAndSet(Subscriptions.CANCELLED) != Subscriptions.CANCELLED) {
                if (onFailure != null) {
                    onFailure.accept(t);
                } else {
                    Infrastructure.handleDroppedException(t);
                }
            } else {
                Infrastructure.handleDroppedException(t);
            }
        }

        @Override
        public void onCompletion() {
            if (subscription.getAndSet(Subscriptions.CANCELLED) != Subscriptions.CANCELLED) {
                if (onCompletion != null) {
                    onCompletion.run();
                }
            }
        }

        @Override
        public void request(long n) {
            subscription.get().request(n);
        }

        @Override
        public void cancel() {
            Subscription prev = subscription.getAndSet(Subscriptions.CANCELLED);
            if (prev != null && prev != Subscriptions.CANCELLED) {
                prev.cancel();
            }
        }
    }
}
