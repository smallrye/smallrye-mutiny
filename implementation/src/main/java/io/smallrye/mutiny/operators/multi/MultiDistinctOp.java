package io.smallrye.mutiny.operators.multi;

import java.util.Collection;
import java.util.HashSet;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;

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
    public void subscribe(Subscriber<? super T> actual) {
        upstream.subscribe(new DistinctProcessor<>(actual));
    }

    static final class DistinctProcessor<T> extends MultiOperatorProcessor<T, T> {

        final Collection<T> collection;

        DistinctProcessor(Subscriber<? super T> downstream) {
            super(downstream);
            this.collection = new HashSet<>();
        }

        @Override
        public void onNext(T t) {
            if (isDone()) {
                return;
            }
            if (collection.add(t)) {
                downstream.onNext(t);
                request(1);
            }
        }

        @Override
        public void onError(Throwable t) {
            super.onError(t);
            collection.clear();
        }

        @Override
        public void onComplete() {
            super.onComplete();
            collection.clear();
        }

        @Override
        public void cancel() {
            super.cancel();
            collection.clear();
        }
    }

}
