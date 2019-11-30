package io.smallrye.mutiny.operators.multi;

import java.util.Objects;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.SwitchableSubscriptionSubscriber;

/**
 * Switches to another Multi if the upstream is empty (completes without having emitted any items).
 */
public final class MultiSwitchOnEmptyOp<T> extends AbstractMultiOperator<T, T> {

    private final Publisher<? extends T> alternative;

    public MultiSwitchOnEmptyOp(Multi<? extends T> upstream, Publisher<? extends T> alternative) {
        super(upstream);
        this.alternative = Objects.requireNonNull(alternative, "alternative");
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        SwitchIfEmptySubscriber<T> parent = new SwitchIfEmptySubscriber<>(actual, alternative);
        actual.onSubscribe(parent);
        upstream.subscribe(parent);
    }

    static final class SwitchIfEmptySubscriber<T> extends SwitchableSubscriptionSubscriber<T> {

        private final Publisher<? extends T> alternative;
        boolean notEmpty;

        SwitchIfEmptySubscriber(Subscriber<? super T> downstream,
                Publisher<? extends T> alternative) {
            super(downstream);
            this.alternative = alternative;
        }

        @Override
        public void onNext(T t) {
            if (!notEmpty) {
                notEmpty = true;
            }
            downstream.onNext(t);
        }

        @Override
        public void onComplete() {
            if (!notEmpty) {
                notEmpty = true;
                alternative.subscribe(this);
            } else {
                downstream.onComplete();
            }
        }
    }
}
