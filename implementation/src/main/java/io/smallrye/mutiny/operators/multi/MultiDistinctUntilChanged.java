package io.smallrye.mutiny.operators.multi;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Returns a result that emits all values pushed by the upstream if they
 * are distinct in comparison to the last value the result upstream emitted.
 * <p>
 * When provided without parameters or with the first parameter,
 * it behaves like this:
 * <p>
 * 1. It will always emit the first value from the source.
 * 2. For all subsequent values pushed by the source, they will be compared to the previously emitted values
 * using the provided `predicate` or an `Objects.equals` equality check.
 * 3. If the value pushed by the source is determined to be unequal by this check, that value is emitted and
 * becomes the new "previously emitted value" internally.
 *
 * @param <T> the type of items
 */
public class MultiDistinctUntilChanged<T> extends AbstractMultiOperator<T, T> {

    private final BiPredicate<T, T> predicate;


    public MultiDistinctUntilChanged(Multi<T> upstream) {
        super(upstream);
        this.predicate = null;
    }

    public MultiDistinctUntilChanged(Multi<T> upstream, BiPredicate<T, T> predicate) {
        super(upstream);
        this.predicate = predicate;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiDistinctUntilChangedProcessor<T>(downstream, this.predicate));
    }


    static final class MultiDistinctUntilChangedProcessor<T> extends MultiOperatorProcessor<T, T> {

        private BiPredicate<T, T> predicate;
        T previousElement;
        //used if the upstream has not emitted yet
        Boolean first;

        MultiDistinctUntilChangedProcessor(MultiSubscriber<? super T> downstream, BiPredicate<T, T> predicate) {
            super(downstream);
            this.predicate = predicate == null ? Objects::equals : predicate;
        }

        @Override
        public void onItem(T currentElement) {
            if (isDone()) {
                return;
            }

            boolean added = false;
            try {
                if (first == null || !predicate.test(previousElement, currentElement)) {
                    first = true;
                    added = true;
                }

                if (added) {
                    this.previousElement = currentElement;
                }


            } catch (Throwable e) {
                // catch exception thrown by the equals / predicate
                failAndCancel(e);
                return;
            }

            if (added) {
                downstream.onItem(currentElement);
            } else {
                request(1);
            }

        }

        @Override
        public void onFailure(Throwable throwable) {
            super.onFailure(throwable);
            this.previousElement = null;
            this.predicate = null;
            this.first = null;
        }

        @Override
        public void onCompletion() {
            super.onCompletion();
            this.previousElement = null;
            this.predicate = null;
            this.first = null;
        }

        @Override
        public void cancel() {
            super.cancel();
            this.previousElement = null;
            this.predicate = null;
            this.first = null;
        }
    }
}
