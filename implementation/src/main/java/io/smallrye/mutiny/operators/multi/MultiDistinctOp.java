package io.smallrye.mutiny.operators.multi;

import java.util.Collection;
import java.util.HashSet;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Eliminates the duplicated items from the upstream.
 *
 * @param <T> the type of items
 */
public final class MultiDistinctOp<T> extends AbstractMultiOperator<T, T> {

    public MultiDistinctOp(Multi<? extends T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        if (actual == null) {
            throw new NullPointerException("Subscriber cannot be `null`");
        }
        upstream.subscribe().withSubscriber(new DistinctProcessor<>(actual));
    }

    static final class DistinctProcessor<T> extends MultiOperatorProcessor<T, T> {

        final Collection<T> collection;

        DistinctProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
            this.collection = new HashSet<>();
        }

        @Override
        public void onItem(T t) {
            if (isDone()) {
                return;
            }

            boolean added;
            try {
                added = collection.add(t);
            } catch (Throwable e) {
                // catch exception thrown by the equals / comparator
                failAndCancel(e);
                return;
            }

            if (added) {
                downstream.onItem(t);
            } else {
                request(1);
            }

        }

        @Override
        public void onFailure(Throwable t) {
            super.onFailure(t);
            collection.clear();
        }

        @Override
        public void onCompletion() {
            super.onCompletion();
            collection.clear();
        }

        @Override
        public void cancel() {
            super.cancel();
            collection.clear();
        }
    }

}
