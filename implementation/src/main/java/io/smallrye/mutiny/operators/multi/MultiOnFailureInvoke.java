package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnFailureInvoke<T> extends AbstractMultiOperator<T, T> {

    private final Consumer<Throwable> callback;
    private final Predicate<? super Throwable> predicate;

    public MultiOnFailureInvoke(Multi<? extends T> upstream, Consumer<Throwable> callback,
            Predicate<? super Throwable> predicate) {
        super(upstream);
        this.callback = callback;
        this.predicate = predicate;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiOnFailureInvokeProcessor(nonNull(downstream, "downstream")));
    }

    class MultiOnFailureInvokeProcessor extends MultiOperatorProcessor<T, T> {

        public MultiOnFailureInvokeProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onFailure(Throwable failure) {
            Flow.Subscription up = getAndSetUpstreamSubscription(CANCELLED);
            MultiSubscriber<? super T> subscriber = downstream;
            if (up != Subscriptions.CANCELLED) {
                try {
                    if (predicate.test(failure)) {
                        callback.accept(failure);
                    }
                } catch (Throwable e) {
                    failure = new CompositeException(failure, e);
                }
                subscriber.onFailure(failure);
            }
        }
    }
}
