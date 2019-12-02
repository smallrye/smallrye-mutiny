
package io.smallrye.mutiny.operators.multi;

import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.SwitchableSubscriptionSubscriber;

public class MultiOnFailureResumeOp<T> extends AbstractMultiOperator<T, T> {

    private final Function<? super Throwable, ? extends Publisher<? extends T>> next;

    public MultiOnFailureResumeOp(Multi<? extends T> upstream,
            Function<? super Throwable, ? extends Publisher<? extends T>> next) {
        super(upstream);
        this.next = ParameterValidation.nonNull(next, "next");
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        upstream.subscribe(new ResumeSubscriber<>(downstream, next));
    }

    static final class ResumeSubscriber<T> extends SwitchableSubscriptionSubscriber<T> {

        private final Function<? super Throwable, ? extends Publisher<? extends T>> next;

        private boolean switched;

        ResumeSubscriber(Subscriber<? super T> downstream,
                Function<? super Throwable, ? extends Publisher<? extends T>> next) {
            super(downstream);
            this.next = next;
        }

        @Override
        public void onSubscribe(Subscription su) {
            if (!switched) {
                downstream.onSubscribe(this);
            }
            super.setOrSwitchUpstream(su);
        }

        @Override
        public void onNext(T item) {
            downstream.onNext(item);

            if (!switched) {
                emitted(1);
            }
        }

        @Override
        public void onError(Throwable failure) {
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
                        super.onError(e);
                    } else {
                        super.onError(new CompositeException(failure, e));
                    }
                    return;
                }
                publisher.subscribe(this);
            } else {
                super.onError(failure);
            }
        }

    }
}
