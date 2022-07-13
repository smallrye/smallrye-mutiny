package io.smallrye.mutiny.subscription;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Objects;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class Subscribers {

    @SuppressWarnings("ThrowableNotThrown")
    public static final Consumer<? super Throwable> NO_ON_FAILURE = failure -> new Exception(
            "Missing onFailure/onError handler in the subscriber", failure).printStackTrace(); // NOSONAR

    public static <T> CancellableSubscriber<T> from(Context context, Consumer<? super T> onItem,
            Consumer<? super Throwable> onFailure,
            Runnable onCompletion,
            Consumer<? super Subscription> onSubscription) {
        return new CallbackBasedSubscriber<>(context, onItem, onFailure, onCompletion, onSubscription);
    }

    public static class CallbackBasedSubscriber<T> implements CancellableSubscriber<T>, Subscription, ContextSupport {

        private volatile Subscription subscription;
        private static final AtomicReferenceFieldUpdater<CallbackBasedSubscriber, Subscription> SUBSCRIPTION_UPDATER = AtomicReferenceFieldUpdater
                .newUpdater(CallbackBasedSubscriber.class, Subscription.class, "subscription");

        private final Context context;
        private final Consumer<? super T> onItem;
        private final Consumer<? super Throwable> onFailure;
        private final Runnable onCompletion;
        private final Consumer<? super Subscription> onSubscription;

        public CallbackBasedSubscriber(
                Context context,
                Consumer<? super T> onItem,
                Consumer<? super Throwable> onFailure,
                Runnable onCompletion,
                Consumer<? super Subscription> onSubscription) {
            this.context = context;
            this.onItem = nonNull(onItem, "onItem");
            this.onFailure = onFailure;
            this.onCompletion = onCompletion;
            this.onSubscription = nonNull(onSubscription, "onSubscription");
        }

        @Override
        public Context context() {
            return this.context;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SUBSCRIPTION_UPDATER.compareAndSet(this, null, s)) {
                try {
                    // onSubscription cannot be null
                    onSubscription.accept(this);
                } catch (Throwable ex) {
                    s.cancel();
                    Infrastructure.handleDroppedException(ex);
                }
            } else {
                s.cancel();
            }
        }

        @Override
        public void onItem(T item) {
            Objects.requireNonNull(item);
            if (SUBSCRIPTION_UPDATER.get(this) != Subscriptions.CANCELLED) {
                try {
                    // onItem cannot be null.
                    onItem.accept(item);
                } catch (Throwable e) {
                    SUBSCRIPTION_UPDATER.getAndSet(this, Subscriptions.CANCELLED).cancel();
                    Infrastructure.handleDroppedException(e);
                }
            }
        }

        @Override
        public void onFailure(Throwable t) {
            Objects.requireNonNull(t);
            if (SUBSCRIPTION_UPDATER.getAndSet(this, Subscriptions.CANCELLED) != Subscriptions.CANCELLED) {
                if (onFailure != null) {
                    try {
                        onFailure.accept(t);
                    } catch (Throwable e) {
                        Infrastructure.handleDroppedException(new CompositeException(t, e));
                    }
                } else {
                    Infrastructure.handleDroppedException(t);
                }
            } else {
                Infrastructure.handleDroppedException(t);
            }
        }

        @Override
        public void onCompletion() {
            if (SUBSCRIPTION_UPDATER.getAndSet(this, Subscriptions.CANCELLED) != Subscriptions.CANCELLED) {
                if (onCompletion != null) {
                    onCompletion.run();
                }
            }
        }

        @Override
        public void request(long n) {
            SUBSCRIPTION_UPDATER.get(this).request(n);
        }

        @Override
        public void cancel() {
            Subscription prev = SUBSCRIPTION_UPDATER.getAndSet(this, Subscriptions.CANCELLED);
            if (prev != null && prev != Subscriptions.CANCELLED) {
                prev.cancel();
            }
        }
    }
}
