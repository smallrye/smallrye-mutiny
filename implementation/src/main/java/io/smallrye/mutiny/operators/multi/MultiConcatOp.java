package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.SwitchableSubscriptionSubscriber;

/**
 * Concatenates a fixed set of Publishers.
 * Items from each publisher are emitted in order.
 * All the items from one publisher must be consumed before items from another publisher are emitted.
 *
 * @param <T> the type of item
 */
public class MultiConcatOp<T> extends AbstractMulti<T> {

    private final Publisher<? extends T>[] publishers;

    private final boolean postponeFailurePropagation;

    @SafeVarargs
    public MultiConcatOp(boolean postponeFailurePropagation, Publisher<? extends T>... publishers) {
        this.publishers = ParameterValidation.doesNotContainNull(publishers, "publishers");
        this.postponeFailurePropagation = postponeFailurePropagation;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        if (actual == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        if (publishers.length == 0) {
            Subscriptions.complete(actual);
            return;
        }

        if (publishers.length == 1) {
            publishers[0].subscribe(Infrastructure.onMultiSubscription(publishers[0], actual));
            return;
        }

        if (postponeFailurePropagation) {
            ConcatArrayAndPostponeFailureSubscriber<T> parent = new ConcatArrayAndPostponeFailureSubscriber<>(actual,
                    publishers);
            actual.onSubscribe(parent);

            if (!parent.isCancelled()) {
                parent.onCompletion();
            }
        } else {
            ConcatArraySubscriber<T> parent = new ConcatArraySubscriber<>(actual, publishers);
            actual.onSubscribe(parent);

            if (!parent.isCancelled()) {
                parent.onCompletion();
            }
        }
    }

    static final class ConcatArraySubscriber<T> extends SwitchableSubscriptionSubscriber<T> {

        private final Publisher<? extends T>[] upstreams;

        private int currentIndex;
        private long emitted;

        private final AtomicInteger wip = new AtomicInteger();

        ConcatArraySubscriber(MultiSubscriber<? super T> actual, Publisher<? extends T>[] upstreams) {
            super(actual);
            this.upstreams = upstreams;
        }

        @Override
        public void onItem(T t) {
            emitted++;
            downstream.onItem(t);
        }

        @Override
        public void onCompletion() {
            if (wip.getAndIncrement() == 0) {
                Publisher<? extends T>[] a = upstreams;
                do {

                    if (isCancelled()) {
                        return;
                    }

                    int i = currentIndex;
                    if (i == a.length) {
                        downstream.onCompletion();
                        return;
                    }

                    Publisher<? extends T> p = a[i];
                    long c = emitted;
                    if (c != 0L) {
                        emitted = 0L;
                        emitted(c);
                    }
                    p.subscribe(Infrastructure.onMultiSubscription(p, this));

                    if (isCancelled()) {
                        return;
                    }

                    currentIndex = ++i;
                } while (wip.decrementAndGet() != 0);
            }

        }
    }

    static final class ConcatArrayAndPostponeFailureSubscriber<T> extends SwitchableSubscriptionSubscriber<T> {

        final Publisher<? extends T>[] upstreams;

        int index;
        long produced;

        private final AtomicInteger wip = new AtomicInteger();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        ConcatArrayAndPostponeFailureSubscriber(MultiSubscriber<? super T> actual, Publisher<? extends T>[] upstreams) {
            super(actual);
            this.upstreams = upstreams;
        }

        @Override
        public void onItem(T t) {
            produced++;
            downstream.onItem(t);
        }

        @Override
        public void onFailure(Throwable t) {
            if (Subscriptions.addFailure(failure, t)) {
                onCompletion();
            }
        }

        @Override
        public void onCompletion() {
            if (wip.getAndIncrement() == 0) {
                Publisher<? extends T>[] a = upstreams;
                do {

                    if (isCancelled()) {
                        return;
                    }

                    int i = index;
                    if (i == a.length) {
                        Throwable last = Subscriptions.markFailureAsTerminated(failure);
                        if (last != null) {
                            downstream.onFailure(last);
                        } else {
                            downstream.onCompletion();
                        }
                        return;
                    }

                    Publisher<? extends T> p = a[i];

                    if (p == null) {
                        downstream.onFailure(
                                new NullPointerException("Source Publisher at currentIndex " + i + " is null"));
                        return;
                    }

                    long c = produced;
                    if (c != 0L) {
                        produced = 0L;
                        emitted(c);
                    }
                    p.subscribe(Infrastructure.onMultiSubscription(p, this));

                    if (isCancelled()) {
                        return;
                    }

                    index = ++i;
                } while (wip.decrementAndGet() != 0);
            }

        }
    }

}
