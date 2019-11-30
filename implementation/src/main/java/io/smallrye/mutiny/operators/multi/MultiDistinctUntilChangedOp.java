package io.smallrye.mutiny.operators.multi;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;

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
    public void subscribe(Subscriber<? super T> actual) {
        upstream.subscribe(new DistinctProcessor<>(actual));
    }

    static final class DistinctProcessor<T> extends MultiOperatorProcessor<T, T> {

        private T last;

        DistinctProcessor(Subscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onNext(T t) {
            if (isDone()) {
                return;
            }
            if (last == null || !last.equals(t)) {
                last = t;
                downstream.onNext(t);
                request(1);
            }
        }

        @Override
        public void onError(Throwable t) {
            super.onError(t);
            last = null;
        }

        @Override
        public void onComplete() {
            super.onComplete();
            last = null;
        }

        @Override
        public void cancel() {
            super.cancel();
            last = null;
        }
    }

}
