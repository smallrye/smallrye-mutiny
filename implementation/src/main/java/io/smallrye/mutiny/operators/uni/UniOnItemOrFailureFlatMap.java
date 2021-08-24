package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.MAPPER_RETURNED_NULL;

import java.util.function.BiFunction;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnItemOrFailureFlatMap<I, O> extends UniOperator<I, O> {

    private final BiFunction<? super I, Throwable, Uni<? extends O>> mapper;

    public UniOnItemOrFailureFlatMap(Uni<I> upstream,
            BiFunction<? super I, Throwable, Uni<? extends O>> mapper) {
        super(upstream);
        this.mapper = mapper;
    }

    @Override
    public void subscribe(UniSubscriber<? super O> subscriber) {
        AbstractUni.subscribe(upstream(), new UniOnItemOrFailureFlatMapProcessor(subscriber));
    }

    private class UniOnItemOrFailureFlatMapProcessor extends UniOperatorProcessor<I, O> {

        private volatile UniSubscription innerSubscription;

        public UniOnItemOrFailureFlatMapProcessor(UniSubscriber<? super O> downstream) {
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
            if (!isCancelled()) {
                if (innerSubscription == null) {
                    performInnerSubscription(item, null);
                } else {
                    downstream.onItem((O) item); // Dirty cast
                }
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (!isCancelled()) {
                if (innerSubscription == null) {
                    performInnerSubscription(null, failure);
                } else {
                    downstream.onFailure(failure);
                }
            } else {
                Infrastructure.handleDroppedException(failure);
            }
        }

        @Override
        public void cancel() {
            if (innerSubscription != null) {
                innerSubscription.cancel();
            }
            super.cancel();
        }

        @SuppressWarnings("unchecked")
        private void performInnerSubscription(I item, Throwable failure) {
            Uni<? extends O> uni;
            try {
                uni = mapper.apply(item, failure);
            } catch (Throwable err) {
                if (failure != null) {
                    downstream.onFailure(new CompositeException(failure, err));
                } else {
                    downstream.onFailure(err);
                }
                return;
            }
            if (uni == null) {
                downstream.onFailure(new NullPointerException(MAPPER_RETURNED_NULL));
                return;
            }
            AbstractUni.subscribe(uni, (UniSubscriber) this); // Dirty cast
        }
    }
}
