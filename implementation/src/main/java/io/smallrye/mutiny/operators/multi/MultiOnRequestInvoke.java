package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.LongConsumer;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnRequestInvoke<T> extends AbstractMultiOperator<T, T> {

    private final LongConsumer consumer;

    public MultiOnRequestInvoke(Multi<? extends T> upstream, LongConsumer consumer) {
        super(upstream);
        this.consumer = consumer;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiOnRequestInvokeOperator(nonNull(downstream, "downstream")));
    }

    class MultiOnRequestInvokeOperator extends MultiOperatorProcessor<T, T> {

        public MultiOnRequestInvokeOperator(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void request(long numberOfItems) {
            try {
                consumer.accept(numberOfItems);
                super.request(numberOfItems);
            } catch (Throwable err) {
                super.onFailure(err);
            }
        }
    }
}
