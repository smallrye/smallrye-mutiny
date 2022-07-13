
package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.SwitchableSubscriptionSubscriber;

public class MultiOnFailureResumeOp<T> extends AbstractMultiOperator<T, T> {

    private final Function<? super Throwable, ? extends Publisher<? extends T>> next;

    public MultiOnFailureResumeOp(Multi<? extends T> upstream,
            Function<? super Throwable, ? extends Publisher<? extends T>> next) {
        super(upstream);
        this.next = ParameterValidation.nonNull(next, "next");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new ResumeSubscriber<>(downstream, next));
    }

    static final class ResumeSubscriber<T> extends SwitchableSubscriptionSubscriber<T> {

        private final Function<? super Throwable, ? extends Publisher<? extends T>> next;

        private boolean switched;

        ResumeSubscriber(MultiSubscriber<? super T> downstream,
                Function<? super Throwable, ? extends Publisher<? extends T>> next) {
            super(downstream);
            this.next = next;
        }

        @Override
        public void onSubscribe(Flow.Subscription su) {
            if (!switched) {
                downstream.onSubscribe(this);
            }
            super.setOrSwitchUpstream(su);
        }

        @Override
        public void onItem(T item) {
            downstream.onItem(item);

            if (!switched) {
                emitted(1);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (!switched) {
                switched = true;
                Publisher<? extends T> publisher;
                try {
                    publisher = next.apply(failure);
                    if (publisher == null) {
                        throw new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL);
                    }
                } catch (Throwable e) {
                    if (e == failure) { // Exception rethrown.
                        super.onFailure(e);
                    } else {
                        super.onFailure(new CompositeException(failure, e));
                    }
                    return;
                }
                publisher.subscribe(Infrastructure.onMultiSubscription(publisher, this));
            } else {
                super.onFailure(failure);
            }
        }

    }
}
