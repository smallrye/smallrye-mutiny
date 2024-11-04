package io.smallrye.mutiny.operators.multi;

import java.util.*;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Eliminates the duplicated items from the upstream.
 *
 * @param <T> the type of items
 * @param <K> the type of key, used to identify duplicates
 */
public final class MultiDistinctByKeyOp<T, K> extends AbstractMultiOperator<T, T> {

    private final Function<T, K> keyExtractor;

    public MultiDistinctByKeyOp(Multi<? extends T> upstream, Function<T, K> keyExtractor) {
        super(upstream);
        this.keyExtractor = keyExtractor;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        upstream.subscribe(
                new DistinctByKeyProcessor<>(ParameterValidation.nonNullNpe(subscriber, "subscriber"), keyExtractor));
    }

    static final class DistinctByKeyProcessor<T, K> extends MultiOperatorProcessor<T, T> {

        private final Collection<K> foundKeys = new HashSet<>();
        private final Function<T, K> keyExtractor;

        DistinctByKeyProcessor(MultiSubscriber<? super T> downstream,
                Function<T, K> keyExtractor) {
            super(downstream);
            this.keyExtractor = keyExtractor;
        }

        @Override
        public void onItem(T t) {
            if (isDone()) {
                return;
            }

            boolean added;
            try {
                added = foundKeys.add(keyExtractor.apply(t));
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
            foundKeys.clear();
        }

        @Override
        public void onCompletion() {
            super.onCompletion();
            foundKeys.clear();
        }

        @Override
        public void cancel() {
            super.cancel();
            foundKeys.clear();
        }
    }

}
