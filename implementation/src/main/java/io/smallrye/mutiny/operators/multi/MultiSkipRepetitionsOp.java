package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNullNpe;

import java.util.Comparator;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Eliminates the duplicated items from the upstream.
 *
 * @param <T> the type of items
 */
public final class MultiSkipRepetitionsOp<T> extends AbstractMultiOperator<T, T> {

    private final Comparator<? super T> comparator;

    public MultiSkipRepetitionsOp(Multi<T> upstream) {
        this(upstream, null);
    }

    public MultiSkipRepetitionsOp(Multi<T> upstream, Comparator<? super T> comparator) {
        super(upstream);
        this.comparator = comparator;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        nonNullNpe(subscriber, "subscriber");
        upstream.subscribe().withSubscriber(new MultiSkipRepetitionsProcessor<>(subscriber, comparator));
    }

    static final class MultiSkipRepetitionsProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final Comparator<? super T> comparator;
        private T last;

        public MultiSkipRepetitionsProcessor(MultiSubscriber<? super T> subscriber, Comparator<? super T> comparator) {
            super(subscriber);
            if (comparator == null) {
                this.comparator = (a, b) -> a.equals(b) ? 0 : 1;
            } else {
                this.comparator = comparator;
            }

        }

        @Override
        public void onItem(T t) {
            if (isDone()) {
                return;
            }
            try {
                if (last == null || comparator.compare(last, t) != 0) {
                    last = t;
                    downstream.onItem(t);
                } else {
                    // Request the next one, as that item is dropped.
                    request(1);
                }
            } catch (Exception e) {
                onFailure(e);
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
