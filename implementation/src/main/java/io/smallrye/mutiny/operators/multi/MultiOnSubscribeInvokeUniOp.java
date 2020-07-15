package io.smallrye.mutiny.operators.multi;

import java.util.Objects;
import java.util.function.Function;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Execute a given callback on subscription.
 * <p>
 * The subscription is only sent downstream once the action completes successfully. If the action fails, the failure is
 * propagated downstream.
 *
 * @param <T> the value type
 */
public final class MultiOnSubscribeInvokeUniOp<T> extends AbstractMultiOperator<T, T> {

    private final Function<? super Subscription, Uni<?>> onSubscribe;

    public MultiOnSubscribeInvokeUniOp(Multi<? extends T> upstream,
            Function<? super Subscription, Uni<?>> onSubscribe) {
        super(upstream);
        this.onSubscribe = onSubscribe;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        if (actual == null) {
            throw new NullPointerException("Subscriber must not be `null`");
        }
        upstream.subscribe().withSubscriber(new OnSubscribeSubscriber(actual));
    }

    private final class OnSubscribeSubscriber extends MultiOperatorProcessor<T, T> {

        OnSubscribeSubscriber(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (upstream.compareAndSet(null, s)) {
                try {
                    Uni<?> uni = Objects.requireNonNull(onSubscribe.apply(s), "The produced Uni must not be `null`");
                    uni
                            .subscribe().with(
                                    ignored -> downstream.onSubscribe(this),
                                    failure -> {
                                        Subscriptions.fail(downstream, failure);
                                        upstream.getAndSet(Subscriptions.CANCELLED).cancel();
                                    });
                } catch (Throwable e) {
                    Subscriptions.fail(downstream, e);
                    upstream.getAndSet(Subscriptions.CANCELLED).cancel();
                }
            } else {
                s.cancel();
            }
        }
    }

}
