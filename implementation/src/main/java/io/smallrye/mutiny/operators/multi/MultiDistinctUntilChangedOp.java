package io.smallrye.mutiny.operators.multi;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Eliminates the duplicated items from the upstream.
 *
 * @param <T> the type of items
 */
public final class MultiDistinctUntilChangedOp<T> extends AbstractMultiOperator<T, T> {

    public MultiDistinctUntilChangedOp(Multi<? extends T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        upstream.subscribe().withSubscriber(new DistinctProcessor<>(actual));
    }

    static final class DistinctProcessor<T> extends MultiOperatorProcessor<T, T> {

        private T last;

        DistinctProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onItem(T t) {
            if (isDone()) {
                return;
            }
            if (last == null || !last.equals(t)) {
                last = t;
                downstream.onItem(t);
            } else {
                // Request the next one, as that item is dropped.
                request(1);
            }
        }

        @Override
        public void onFailure(Throwable t) {
            super.onFailure(t);
            last = null;
        }

        @Override
        public void onCompletion() {
            super.onCompletion();
            last = null;
        }

        @Override
        public void cancel() {
            super.cancel();
            last = null;
        }
    }

}
