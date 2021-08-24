package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.MAPPER_RETURNED_NULL;

import java.util.function.Function;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnItemTransformToUni<I, O> extends UniOperator<I, O> {

    private final Function<? super I, Uni<? extends O>> mapper;

    public UniOnItemTransformToUni(Uni<I> upstream, Function<? super I, Uni<? extends O>> mapper) {
        super(upstream);
        this.mapper = mapper;
    }

    @Override
    public void subscribe(UniSubscriber<? super O> subscriber) {
        AbstractUni.subscribe(upstream(), new UniOnItemTransformToUniProcessor(subscriber));
    }

    // Note: serves as a subscription/subscriber for both the upstream and the Uni.
    // There was an earlier design with a nested processor, but this requires an extra allocation.
    private class UniOnItemTransformToUniProcessor extends UniOperatorProcessor<I, O> {

        private volatile UniSubscription innerSubscription;

        public UniOnItemTransformToUniProcessor(UniSubscriber<? super O> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(UniSubscription subscription) {
            if (getCurrentUpstreamSubscription() == null) {
                super.onSubscribe(subscription);
            } else if (innerSubscription == null) {
                this.innerSubscription = subscription;
            } else {
                subscription.cancel();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onItem(I item) {
            if (isCancelled()) {
                return;
            }
            if (innerSubscription == null) {
                // Input item from upstream
                performInnerSubscription(item);
            } else {
                // Output item from the inner Uni
                downstream.onItem((O) item); // not a pretty cast
            }
        }

        @SuppressWarnings("unchecked")
        private void performInnerSubscription(I item) {
            Uni<? extends O> uni;
            try {
                uni = mapper.apply(item);
            } catch (Throwable e) {
                if (item instanceof Throwable) {
                    downstream.onFailure(new CompositeException((Throwable) item, e));
                } else {
                    downstream.onFailure(e);
                }
                return;
            }
            if (uni == null) {
                downstream.onFailure(new NullPointerException(MAPPER_RETURNED_NULL));
                return;
            }
            AbstractUni.subscribe(uni, (UniSubscriber) this); // not a pretty cast
        }

        @Override
        public void cancel() {
            if (innerSubscription != null) {
                innerSubscription.cancel();
            }
            super.cancel();
        }
    }
}
