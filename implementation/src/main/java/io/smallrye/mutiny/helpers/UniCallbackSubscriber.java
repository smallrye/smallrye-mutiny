package io.smallrye.mutiny.helpers;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * Implementation of a {@link UniSubscriber} based on callbacks.
 * This implementation also implement {@link UniSubscription} to expose the {@link #cancel()} method.
 *
 * @param <T> the type of item received by this subscriber
 */
public class UniCallbackSubscriber<T> implements UniSubscriber<T>, UniSubscription {

    private volatile UniSubscription subscription;
    private static final AtomicReferenceFieldUpdater<UniCallbackSubscriber, UniSubscription> SUBSCRIPTION_UPDATER = AtomicReferenceFieldUpdater
            .newUpdater(UniCallbackSubscriber.class, UniSubscription.class, "subscription");

    private final Consumer<? super T> onResultCallback;
    private final Consumer<? super Throwable> onFailureCallback;
    private final Context context;

    /**
     * Creates a {@link UniSubscriber} consuming the item and failure of a
     * {@link Uni}.
     *
     * @param onResultCallback callback invoked on item event, must not be {@code null}
     * @param onFailureCallback callback invoked on failure event, must not be {@code null}
     * @param context the subscriber context, must not be {@code null}
     */
    public UniCallbackSubscriber(Consumer<? super T> onResultCallback,
            Consumer<? super Throwable> onFailureCallback,
            Context context) {
        this.onResultCallback = nonNull(onResultCallback, "onResultCallback");
        this.onFailureCallback = nonNull(onFailureCallback, "onFailureCallback");
        this.context = nonNull(context, "context");
    }

    @Override
    public final void onSubscribe(UniSubscription sub) {
        if (!SUBSCRIPTION_UPDATER.compareAndSet(this, null, sub)) {
            // cancelling this second subscription
            // because we already add a subscription (maybe CANCELLED)
            sub.cancel();
        }
    }

    @Override
    public final void onFailure(Throwable t) {
        if (SUBSCRIPTION_UPDATER.getAndSet(this, CANCELLED) == CANCELLED) {
            // Already cancelled, do nothing
            return;
        }
        onFailureCallback.accept(t);
    }

    @Override
    public final void onItem(T x) {
        if (SUBSCRIPTION_UPDATER.getAndSet(this, CANCELLED) == CANCELLED) {
            // Already cancelled, do nothing
            return;
        }

        try {
            onResultCallback.accept(x);
        } catch (Throwable t) {
            Infrastructure.handleDroppedException(t);
        }
    }

    @Override
    public void cancel() {
        UniSubscription sub = SUBSCRIPTION_UPDATER.getAndSet(this, CANCELLED);
        if (sub != CANCELLED) {
            sub.cancel();
        }
    }

    @Override
    public Context context() {
        return context;
    }
}
