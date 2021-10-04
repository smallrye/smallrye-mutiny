package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnItemInvoke<T> extends AbstractMultiOperator<T, T> {

    private final Consumer<? super T> callback;

    public MultiOnItemInvoke(Multi<? extends T> upstream, Consumer<? super T> callback) {
        super(upstream);
        this.callback = callback;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiOnItemInvokeProcessor(nonNull(downstream, "downstream")));
    }

    class MultiOnItemInvokeProcessor extends MultiOperatorProcessor<T, T> {

        public MultiOnItemInvokeProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onItem(T item) {
            if (getUpstreamSubscription() != Subscriptions.CANCELLED) {
                MultiSubscriber<? super T> subscriber = this.downstream;
                try {
                    callback.accept(item);
                } catch (Throwable t) {
                    failAndCancel(t);
                    return;
                }
                subscriber.onItem(item);
            }
        }
    }
}
