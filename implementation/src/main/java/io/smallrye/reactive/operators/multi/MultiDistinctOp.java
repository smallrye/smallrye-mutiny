package io.smallrye.reactive.operators.multi;

import io.smallrye.reactive.Multi;
import org.reactivestreams.Subscriber;

import java.util.Collection;
import java.util.HashSet;

/**
 * Eliminates the duplicated items from the upstream.
 *
 * @param <T> the type of items
 */
public final class MultiDistinctOp<T> extends AbstractMultiWithUpstream<T, T> {

    public MultiDistinctOp(Multi<? extends T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        upstream.subscribe(new DistinctSubscriber<>(actual));
    }

    static final class DistinctSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        final Collection<T> collection;

        DistinctSubscriber(Subscriber<? super T> downstream) {
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
