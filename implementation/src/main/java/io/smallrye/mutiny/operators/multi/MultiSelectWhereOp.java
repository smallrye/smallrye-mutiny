package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.concurrent.Flow.Subscription;
import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Filters out items from the upstream that do <strong>NOT</strong> pass the given filter.
 *
 * @param <T> the type of item
 */
public class MultiSelectWhereOp<T> extends AbstractMultiOperator<T, T> {

    private final Predicate<? super T> predicate;

    public MultiSelectWhereOp(Multi<? extends T> upstream, Predicate<? super T> predicate) {
        super(upstream);
        this.predicate = ParameterValidation.nonNull(predicate, "predicate");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        ParameterValidation.nonNullNpe(subscriber, "subscriber");
        upstream.subscribe().withSubscriber(new MultiSelectWhereProcessor<>(subscriber, predicate));
    }

    static final class MultiSelectWhereProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final Predicate<? super T> predicate;
        private boolean requestedMax = false;

        MultiSelectWhereProcessor(MultiSubscriber<? super T> downstream, Predicate<? super T> predicate) {
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
            } else if (!requestedMax) {
                request(1);
            }
        }

        @Override
        public void request(long numberOfItems) {
            Subscription subscription = getUpstreamSubscription();
            if (subscription != CANCELLED) {
                if (numberOfItems <= 0) {
                    onFailure(new IllegalArgumentException("Invalid number of request, must be greater than 0"));
                    return;
                }
                if (numberOfItems == Long.MAX_VALUE) {
                    requestedMax = true;
                }
                subscription.request(numberOfItems);
            }
        }
    }
}
