package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.Objects;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Execute a given (async) callback on subscription.
 * <p>
 * The subscription is only sent downstream once the action completes successfully (provides an item, potentially
 * {@code null}). If the action fails, the failure is propagated downstream.
 *
 * @param <T> the value type
 */
public final class MultiOnSubscribeInvokeOp<T> extends AbstractMultiOperator<T, T> {

    private final Consumer<? super Subscription> onSubscribe;

    public MultiOnSubscribeInvokeOp(Multi<? extends T> upstream,
            Consumer<? super Subscription> onSubscribe) {
        super(upstream);
        this.onSubscribe = onSubscribe;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        OnSubscribeSubscriber subscriber = new OnSubscribeSubscriber(
                Objects.requireNonNull(actual, "Subscriber must not be `null`"));
        upstream.subscribe().withSubscriber(subscriber);
    }

    private final class OnSubscribeSubscriber extends MultiOperatorProcessor<T, T> {

        OnSubscribeSubscriber(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (compareAndSetUpstreamSubscription(null, s)) {
                try {
                    onSubscribe.accept(s);
                } catch (Throwable e) {
                    Subscriptions.fail(downstream, e);
                    getAndSetUpstreamSubscription(CANCELLED).cancel();
                    return;
                }
                downstream.onSubscribe(this);
            } else {
                s.cancel();
            }
        }

    }

}
