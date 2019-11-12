package io.smallrye.reactive.operators.multi;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.operators.AbstractMulti;
import io.smallrye.reactive.subscription.SwitchableSubscriptionSubscriber;

/**
 * Concatenates a fixed set of Publishers.
 * Items from each publisher are emitted in order.
 * All the imtes from one publisher must be consumed before items from another publisher are emitted.
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
    public void subscribe(Subscriber<? super T> actual) {
        if (publishers.length == 0) {
            Subscriptions.complete(actual);
            return;
        }

        if (publishers.length == 1) {
            publishers[0].subscribe(actual);
            return;
        }

        if (postponeFailurePropagation) {
            ConcatArrayAndPostponeFailureSubscriber<T> parent = new ConcatArrayAndPostponeFailureSubscriber<>(actual,
                    publishers);
            actual.onSubscribe(parent);

            if (!parent.isCancelled()) {
                parent.onComplete();
            }
        } else {
            ConcatArraySubscriber<T> parent = new ConcatArraySubscriber<>(actual, publishers);
            actual.onSubscribe(parent);

            if (!parent.isCancelled()) {
                parent.onComplete();
            }
        }
    }

    /**
     * Returns a new instance of {@link MultiConcatOp} which concatenates items from the current set of upstreams and the
     * passed upstream (appended at the end).
     * <p>
     * This operation doesn't change the current instance but produce a new one.
     *
     * @param upstream the new upstream, must not be {@code null}
     * @return the new MultiConcatOp instance
     */
    @SuppressWarnings("unchecked")
    MultiConcatOp<T> appendUpstream(Publisher<? extends T> upstream) {
        ParameterValidation.nonNull(upstream, "upstream");
        int n = publishers.length;
        Publisher<? extends T>[] newArray = new Publisher[n + 1];
        System.arraycopy(publishers, 0, newArray, 0, n);
        newArray[n] = upstream;
        return new MultiConcatOp<>(postponeFailurePropagation, newArray);
    }

    /**
     * Returns a new instance of {@link MultiConcatOp} which concatenates items from the passed upstream followed with
     * items from the current set of upstreams.
     * <p>
     * This operation doesn't change the current instance but produce a new one.
     *
     * @param upstream the new upstream, must not be {@code null}
     * @return the new MultiConcatOp instance
     */
    @SuppressWarnings("unchecked")
    MultiConcatOp<T> prependUpstream(Publisher<? extends T> upstream) {
        int n = publishers.length;
        Publisher<? extends T>[] newArray = new Publisher[n + 1];
        System.arraycopy(publishers, 0, newArray, 1, n);
        newArray[0] = upstream;
        return new MultiConcatOp<>(postponeFailurePropagation, newArray);
    }

    @Override
    protected Publisher<T> publisher() {
        return this;
    }

    static final class ConcatArraySubscriber<T> extends SwitchableSubscriptionSubscriber<T> {

        private final Publisher<? extends T>[] upstreams;

        private int currentIndex;
        private long emitted;

        private final AtomicInteger wip = new AtomicInteger();

        ConcatArraySubscriber(Subscriber<? super T> actual, Publisher<? extends T>[] upstreams) {
            super(actual);
            this.upstreams = upstreams;
        }

        @Override
        public void onNext(T t) {
            emitted++;
            downstream.onNext(t);
        }

        @Override
        public void onComplete() {
            if (wip.getAndIncrement() == 0) {
                Publisher<? extends T>[] a = upstreams;
                do {

                    if (isCancelled()) {
                        return;
                    }

                    int i = currentIndex;
                    if (i == a.length) {
                        downstream.onComplete();
                        return;
                    }

                    Publisher<? extends T> p = a[i];
                    long c = emitted;
                    if (c != 0L) {
                        emitted = 0L;
                        emitted(c);
                    }
                    p.subscribe(this);

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

        ConcatArrayAndPostponeFailureSubscriber(Subscriber<? super T> actual, Publisher<? extends T>[] upstreams) {
            super(actual);
            this.upstreams = upstreams;
        }

        @Override
        public void onNext(T t) {
            produced++;
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (Subscriptions.addFailure(failure, t)) {
                onComplete();
            }
        }

        @Override
        public void onComplete() {
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
                            downstream.onError(last);
                        } else {
                            downstream.onComplete();
                        }
                        return;
                    }

                    Publisher<? extends T> p = a[i];

                    if (p == null) {
                        downstream.onError(
                                new NullPointerException("Source Publisher at currentIndex " + i + " is null"));
                        return;
                    }

                    long c = produced;
                    if (c != 0L) {
                        produced = 0L;
                        emitted(c);
                    }
                    p.subscribe(this);

                    if (isCancelled()) {
                        return;
                    }

                    index = ++i;
                } while (wip.decrementAndGet() != 0);
            }

        }
    }

}
