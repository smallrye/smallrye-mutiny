package io.smallrye.mutiny.operators.multi.builders;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;

public class CollectionBasedMulti<T> extends AbstractMulti<T> {

    /**
     * The collection, immutable once set.
     */
    private final Collection<T> collection;

    @SafeVarargs
    public CollectionBasedMulti(T... array) {
        this.collection = Arrays.asList(ParameterValidation.doesNotContainNull(array, "array"));
    }

    public CollectionBasedMulti(Collection<T> collection) {
        this.collection = Collections.unmodifiableCollection(
                ParameterValidation.doesNotContainNull(collection, "collection"));
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        ParameterValidation.nonNullNpe(actual, "subscriber");
        if (collection.isEmpty()) {
            Subscriptions.complete(actual);
            return;
        }
        actual.onSubscribe(new CollectionSubscription<>(actual, collection));
    }

    @Override
    protected Publisher<T> publisher() {
        return this;
    }

    public static final class CollectionSubscription<T> implements Subscription {

        private final Subscriber<? super T> downstream;
        private final List<T> collection; // Immutable
        private int index;

        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicLong requested = new AtomicLong();

        public CollectionSubscription(Subscriber<? super T> downstream, Collection<T> collection) {
            this.downstream = downstream;
            this.collection = new ArrayList<>(collection);
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                if (Subscriptions.add(requested, n) == 0) {
                    if (n == Long.MAX_VALUE) {
                        produceWithoutBackPressure();
                    } else {
                        followRequests(n);
                    }
                }
            } else {
                downstream.onError(Subscriptions.getInvalidRequestException());
            }
        }

        void followRequests(long n) {
            final List<T> items = collection;
            final int size = items.size();

            int current = index;
            int emitted = 0;

            for (;;) {
                if (cancelled.get()) {
                    return;
                }

                while (current != size && emitted != n) {
                    downstream.onNext(items.get(current));

                    if (cancelled.get()) {
                        return;
                    }

                    current++;
                    emitted++;
                }

                if (current == size) {
                    downstream.onComplete();
                    return;
                }

                n = requested.get();

                if (n == emitted) {
                    index = current;
                    n = requested.addAndGet(-emitted);
                    if (n == 0) {
                        return;
                    }
                    emitted = 0;
                }
            }
        }

        void produceWithoutBackPressure() {
            for (T item : collection) {
                if (cancelled.get()) {
                    return;
                }
                downstream.onNext(item);
            }

            if (cancelled.get()) {
                return;
            }
            downstream.onComplete();
        }

        @Override
        public void cancel() {
            cancelled.set(true);
        }
    }

}
