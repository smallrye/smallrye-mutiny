package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.LongFunction;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.MultiOperator;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiDemandCapping<T> extends MultiOperator<T, T> {

    private final LongFunction<Long> function;

    public MultiDemandCapping(Multi<T> upstream, LongFunction<Long> function) {
        super(upstream);
        this.function = function;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        upstream().subscribe(new MultiDemandCappingProcessor(subscriber));
    }

    private class MultiDemandCappingProcessor extends MultiOperatorProcessor<T, T> {

        MultiDemandCappingProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void request(long numberOfItems) {
            Subscription subscription = getUpstreamSubscription();
            if (subscription == Subscriptions.CANCELLED) {
                return;
            }
            if (numberOfItems <= 0) {
                onFailure(new IllegalArgumentException("Invalid number of request, must be greater than 0"));
                return;
            }
            try {
                long actualDemand = nonNull(function.apply(numberOfItems), "actualDemand");
                if (actualDemand <= 0) {
                    onFailure(new IllegalArgumentException("Invalid number of request, must be greater than 0"));
                    return;
                }
                if (actualDemand > numberOfItems) {
                    onFailure(new IllegalStateException("The demand capping function computed a request of " + actualDemand
                            + " elements while the downstream request is of " + numberOfItems + " elements"));
                    return;
                }
                subscription.request(actualDemand);
            } catch (Throwable failure) {
                onFailure(failure);
            }
        }
    }
}
