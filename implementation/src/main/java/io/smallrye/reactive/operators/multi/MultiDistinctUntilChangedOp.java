package io.smallrye.reactive.operators.multi;

import io.smallrye.reactive.Multi;
import org.reactivestreams.Subscriber;

/**
 * Eliminates the duplicated items from the upstream.
 *
 * @param <T> the type of items
 */
public final class MultiDistinctUntilChangedOp<T> extends AbstractMultiWithUpstream<T, T> {

    public MultiDistinctUntilChangedOp(Multi<? extends T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        upstream.subscribe(new DistinctSubscriber<>(actual));
    }

    static final class DistinctSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        private T last;

        DistinctSubscriber(Subscriber<? super T> downstream) {
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
