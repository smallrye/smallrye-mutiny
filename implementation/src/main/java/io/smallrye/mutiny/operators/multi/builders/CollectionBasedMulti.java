package io.smallrye.mutiny.operators.multi.builders;

import java.util.*;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

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
    public void subscribe(MultiSubscriber<? super T> actual) {
        ParameterValidation.nonNullNpe(actual, "subscriber");
        if (collection.isEmpty()) {
            Subscriptions.complete(actual);
            return;
        }
        actual.onSubscribe(new CollectionSubscription<>(actual, collection));
    }

    private static final class CollectionSubscription<T> implements Flow.Subscription {

        private final MultiSubscriber<? super T> downstream;
        private final List<T> collection; // Immutable
        private int index;

        private volatile boolean cancelled;
        AtomicLong requested = new AtomicLong();

        public CollectionSubscription(MultiSubscriber<? super T> downstream, Collection<T> collection) {
            this.downstream = downstream;
            this.collection = wrapIfNotList(collection);
        }

        private List<T> wrapIfNotList(Collection<T> collection) {
            if (collection instanceof List) {
                return (List<T>) collection;
            } else {
                return new ArrayList<>(collection);
            }
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
                downstream.onFailure(Subscriptions.getInvalidRequestException());
            }
        }

        void followRequests(long n) {
            final List<T> items = collection;
            final int size = items.size();

            int current = index;
            int emitted = 0;

            for (;;) {
                if (cancelled) {
                    return;
                }

                while (current != size && emitted != n) {
                    downstream.onItem(items.get(current));

                    if (cancelled) {
                        return;
                    }

                    current++;
                    emitted++;
                }

                if (current == size) {
                    downstream.onCompletion();
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
            // We must be sure we don't replay already send item, so we must skip "index" items
            int size = collection.size();
            for (int i = index; i < size; i++) {
                T item = collection.get(i);
                if (cancelled) {
                    return;
                }
                downstream.onItem(item);
            }

            if (cancelled) {
                return;
            }
            downstream.onCompletion();
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

}
