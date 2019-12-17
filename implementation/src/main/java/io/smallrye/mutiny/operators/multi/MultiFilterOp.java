package io.smallrye.mutiny.operators.multi;

import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Filters out items from the upstream that do <strong>NOT</strong> pass the given filter.
 *
 * @param <T> the type of item
 */
public class MultiFilterOp<T> extends AbstractMultiOperator<T, T> {

    private final Predicate<? super T> predicate;

    public MultiFilterOp(Multi<? extends T> upstream, Predicate<? super T> predicate) {
        super(upstream);
        this.predicate = ParameterValidation.nonNull(predicate, "predicate");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        if (downstream == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        upstream.subscribe().withSubscriber(new MultiFilterProcessor<>(downstream, predicate));
    }

    static final class MultiFilterProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final Predicate<? super T> predicate;

        MultiFilterProcessor(MultiSubscriber<? super T> downstream, Predicate<? super T> predicate) {
            super(downstream);
            this.predicate = predicate;
        }

        @Override
        public void onItem(T t) {
            if (isDone()) {
                return;
            }

            boolean passed;
            try {
                passed = predicate.test(t);
            } catch (Throwable exception) {
                failAndCancel(exception);
                return;
            }

            if (passed) {
                downstream.onItem(t);
            } else {
                request(1);
            }
        }
    }
}
