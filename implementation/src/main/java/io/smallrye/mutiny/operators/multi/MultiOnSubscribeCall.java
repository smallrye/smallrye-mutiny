package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.function.Function;

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
public final class MultiOnSubscribeCall<T> extends AbstractMultiOperator<T, T> {

    private final Function<? super Flow.Subscription, Uni<?>> onSubscribe;

    public MultiOnSubscribeCall(Multi<? extends T> upstream,
            Function<? super Flow.Subscription, Uni<?>> onSubscribe) {
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
        public void onSubscribe(Flow.Subscription s) {
            if (compareAndSetUpstreamSubscription(null, s)) {
                try {
                    Uni<?> uni = Objects.requireNonNull(onSubscribe.apply(s), "The produced Uni must not be `null`");
                    uni
                            .subscribe().with(
                                    ignored -> downstream.onSubscribe(this),
                                    failure -> {
                                        Subscriptions.fail(downstream, failure);
                                        getAndSetUpstreamSubscription(CANCELLED).cancel();
                                    });
                } catch (Throwable e) {
                    Subscriptions.fail(downstream, e);
                    getAndSetUpstreamSubscription(CANCELLED).cancel();
                }
            } else {
                s.cancel();
            }
        }
    }

}
