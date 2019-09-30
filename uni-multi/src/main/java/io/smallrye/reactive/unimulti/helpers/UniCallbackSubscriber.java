package io.smallrye.reactive.unimulti.helpers;

import static io.smallrye.reactive.unimulti.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.reactivestreams.Subscription;

import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.subscription.UniSubscriber;
import io.smallrye.reactive.unimulti.subscription.UniSubscription;

/**
 * Implementation of a {@link UniSubscriber} based on callbacks.
 * This implementation also implement {@link UniSubscription} to expose the {@link #cancel()} method.
 *
 * @param <T> the type of item received by this subscriber
 */
public class UniCallbackSubscriber<T> implements UniSubscriber<T>, UniSubscription {

    private final AtomicReference<UniSubscription> subscription = new AtomicReference<>();
    private final Consumer<? super T> onResultCallback;
    private final Consumer<? super Throwable> onFailureCallback;

    /**
     * Creates a {@link UniSubscriber} consuming the item and failure of a
     * {@link Uni}.
     *
     * @param onResultCallback callback invoked on item event, must not be {@code null}
     * @param onFailureCallback callback invoked on failure event, must not be {@code null}
     */
    public UniCallbackSubscriber(Consumer<? super T> onResultCallback,
            Consumer<? super Throwable> onFailureCallback) {
        this.onResultCallback = nonNull(onResultCallback, "onResultCallback");
        this.onFailureCallback = nonNull(onFailureCallback, "onFailureCallback");
    }

    @Override
    public final void onSubscribe(UniSubscription sub) {
        if (!subscription.compareAndSet(null, sub)) {
            // cancelling this second subscription
            // because we already add a subscription (maybe CANCELLED)
            sub.cancel();
        }
    }

    @Override
    public final void onFailure(Throwable t) {
        UniSubscription sub = subscription.getAndSet(CANCELLED);
        if (sub == CANCELLED) {
            // Already cancelled, do nothing
            return;
        }
        onFailureCallback.accept(t);
    }

    @Override
    public final void onItem(T x) {
        Subscription sub = subscription.getAndSet(CANCELLED);
        if (sub == CANCELLED) {
            // Already cancelled, do nothing
            return;
        }

        try {
            onResultCallback.accept(x);
        } catch (Throwable t) {
            // TODO Log this, or collect the failure
        }
    }

    @Override
    public void cancel() {
        Subscription sub = subscription.getAndSet(CANCELLED);
        if (sub != null) {
            sub.cancel();
        }
    }
}
