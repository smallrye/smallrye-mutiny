package io.smallrye.mutiny.operators.multi;

import java.util.*;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Eliminates the duplicated items from the upstream.
 *
 * @param <T> the type of items
 */
public final class MultiDistinctOp<T> extends AbstractMultiOperator<T, T> {

    private final Comparator<? super T> comparator;

    public MultiDistinctOp(Multi<? extends T> upstream) {
        this(upstream, null);
    }

    public MultiDistinctOp(Multi<? extends T> upstream, Comparator<? super T> comparator) {
        super(upstream);
        this.comparator = comparator;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        upstream.subscribe(new DistinctProcessor<>(ParameterValidation.nonNullNpe(subscriber, "subscriber"), comparator));
    }

    static final class DistinctProcessor<T> extends MultiOperatorProcessor<T, T> {

        final Collection<T> collection;

        DistinctProcessor(MultiSubscriber<? super T> downstream, Comparator<? super T> comparator) {
            super(downstream);
            if (comparator == null) {
                this.collection = new HashSet<>();
            } else {
                this.collection = new TreeSet<>(comparator);
            }
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
