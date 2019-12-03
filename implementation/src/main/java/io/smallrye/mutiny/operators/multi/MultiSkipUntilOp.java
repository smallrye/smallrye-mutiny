package io.smallrye.mutiny.operators.multi;

import java.util.function.Predicate;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;

/**
 * Skips the items from upstream until the passed predicates returns {@code true}.
 *
 * @param <T> the type of item
 */
public final class MultiSkipUntilOp<T> extends AbstractMultiOperator<T, T> {

    private final Predicate<? super T> predicate;

    public MultiSkipUntilOp(Multi<? extends T> upstream, Predicate<? super T> predicate) {
        super(upstream);
        this.predicate = ParameterValidation.nonNull(predicate, "predicate");
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        ParameterValidation.nonNullNpe(actual, "subscriber");
        upstream.subscribe(new SkipUntilProcessor<>(actual, predicate));
    }

    static final class SkipUntilProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final Predicate<? super T> predicate;
        private boolean gateOpen = false;

        SkipUntilProcessor(Subscriber<? super T> downstream, Predicate<? super T> predicate) {
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
                failAndCancel(e);
                return;
            }

            if (!toBeSkipped) {
                gateOpen = true;
                downstream.onNext(t);
                return;
            }

            request(1);
        }
    }

}
