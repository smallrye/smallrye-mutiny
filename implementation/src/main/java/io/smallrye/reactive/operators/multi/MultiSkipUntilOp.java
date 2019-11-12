package io.smallrye.reactive.operators.multi;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;
import org.reactivestreams.Subscriber;

import java.util.function.Predicate;

/**
 * Skips the items from upstream until the passed predicates returns {@code true}.
 *
 * @param <T> the type of item
 */
public final class MultiSkipUntilOp<T> extends AbstractMultiWithUpstream<T, T> {

    private final Predicate<? super T> predicate;

    public MultiSkipUntilOp(Multi<? extends T> upstream, Predicate<? super T> predicate) {
        super(upstream);
        this.predicate = ParameterValidation.nonNull(predicate, "predicate");
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        upstream.subscribe(new SkipUntilSubscriber<>(actual, predicate));
    }

    static final class SkipUntilSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        private final Predicate<? super T> predicate;
        private boolean gateOpen = false;

        SkipUntilSubscriber(Subscriber<? super T> downstream, Predicate<? super T> predicate) {
            super(downstream);
            this.predicate = predicate;
        }

        @Override
        public void onNext(T t) {
            if (isDone()) {
                return;
            }

            if (gateOpen) {
                downstream.onNext(t);
                return;
            }

            boolean toBeSkipped;
            try {
                toBeSkipped = predicate.test(t);
            } catch (Throwable e) {
                onError(e);
                return;
            }

            if (! toBeSkipped) {
                gateOpen = true;
                downstream.onNext(t);
                return;
            }

            request(1);
        }
    }

}
