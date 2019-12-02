package io.smallrye.mutiny.operators.multi;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

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
        if (actual == null) {
            throw new NullPointerException("Subscriber cannot be `null`");
        }
        upstream.subscribe(new DistinctProcessor<>(actual));
    }

    static final class DistinctProcessor<T> extends MultiOperatorProcessor<T, T> {

        final Collection<T> collection;
        final AtomicLong requested = new AtomicLong();

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
            } else {
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
