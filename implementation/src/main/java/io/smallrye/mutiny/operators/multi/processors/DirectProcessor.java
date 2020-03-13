package io.smallrye.mutiny.operators.multi.processors;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.Subscriptions;

/**
 * Dispatches onNext, onError and onComplete signals to zero-to-many Subscribers. Please note, that along with multiple
 * consumers, it supports multiple producers. However, all producers must produce messages on the same Thread, otherwise
 * it would not be compliant with the Reactive Streams spec.
 * <p>
 * The {@link DirectProcessor} does not coordinate back-pressure between its Subscribers and the upstream, but consumes
 * its upstream in an unbounded manner. In the case where a downstream Subscriber is not ready to receive items (hasn't
 * requested yet or enough), it will be terminated with an <i>{@link IllegalStateException}</i>.
 * <p>
 * If there are no Subscribers, upstream items are dropped and only the terminal events are retained. A terminated
 * DirectProcessor will emit the terminal signal to late subscribers.
 *
 * @param <T> the input and output type of item
 */
public final class DirectProcessor<T> implements Processor<T, T> {

    /**
     * Create a new {@link DirectProcessor}
     *
     * @param <E> Type of processed signals
     * @return a fresh processor
     */
    public static <E> DirectProcessor<E> create() {
        return new DirectProcessor<>();
    }

    @SuppressWarnings("rawtypes")
    private static final DirectInner[] EMPTY = new DirectInner[0];

    @SuppressWarnings("rawtypes")
    private static final DirectInner[] TERMINATED = new DirectInner[0];

    @SuppressWarnings("unchecked")
    private AtomicReference<DirectInner<T>[]> subscribers = new AtomicReference<>(EMPTY);

    Throwable failure;

    @Override
    public void onSubscribe(Subscription s) {
        Objects.requireNonNull(s, "s");
        if (subscribers.get() != TERMINATED) {
            s.request(Long.MAX_VALUE);
        } else {
            s.cancel();
        }
    }

    @Override
    public void onNext(T t) {
        Objects.requireNonNull(t, "t");

        DirectInner<T>[] inners = subscribers.get();

        if (inners == TERMINATED) {
            return;
        }

        for (DirectInner<T> s : inners) {
            s.onNext(t);
        }
    }

    @Override
    public void onError(Throwable failure) {
        Objects.requireNonNull(failure, "failure");
        DirectInner<T>[] inners = subscribers.get();

        if (inners == TERMINATED) {
            return;
        }

        this.failure = failure;
        for (DirectInner<?> s : subscribers.getAndSet(TERMINATED)) {
            s.onError(failure);
        }
    }

    @Override
    public void onComplete() {
        for (DirectInner<?> s : subscribers.getAndSet(TERMINATED)) {
            s.onComplete();
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        Objects.requireNonNull(actual, "actual");

        DirectInner<T> p = new DirectInner<>(actual, this);
        actual.onSubscribe(p);

        if (add(p)) {
            if (p.cancelled) {
                remove(p);
            }
        } else {
            Throwable e = failure;
            if (e != null) {
                actual.onError(e);
            } else {
                actual.onComplete();
            }
        }
    }

    boolean add(DirectInner<T> s) {
        DirectInner<T>[] a = subscribers.get();
        if (a == TERMINATED) {
            return false;
        }

        synchronized (this) {
            a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }
            int len = a.length;

            @SuppressWarnings("unchecked")
            DirectInner<T>[] b = new DirectInner[len + 1];
            System.arraycopy(a, 0, b, 0, len);
            b[len] = s;

            subscribers.set(b);

            return true;
        }
    }

    @SuppressWarnings("unchecked")
    void remove(DirectInner<T> s) {
        DirectInner<T>[] a = subscribers.get();
        if (a == TERMINATED || a == EMPTY) {
            return;
        }

        synchronized (this) {
            a = subscribers.get();
            if (a == TERMINATED || a == EMPTY) {
                return;
            }
            int len = a.length;

            int j = -1;

            for (int i = 0; i < len; i++) {
                if (a[i] == s) {
                    j = i;
                    break;
                }
            }
            if (j < 0) {
                return;
            }
            if (len == 1) {
                subscribers.set(EMPTY);
                return;
            }

            DirectInner<T>[] b = new DirectInner[len - 1];
            System.arraycopy(a, 0, b, 0, j);
            System.arraycopy(a, j + 1, b, j, len - j - 1);

            subscribers.set(b);
        }
    }

    public Throwable getFailure() {
        if (subscribers.get() == TERMINATED) {
            return failure;
        }
        return null;
    }

    static final class DirectInner<T> implements Subscription {

        final Subscriber<? super T> downstream;

        final DirectProcessor<T> parent;

        volatile boolean cancelled;

        private final AtomicLong requested = new AtomicLong();

        DirectInner(Subscriber<? super T> downstream, DirectProcessor<T> parent) {
            this.downstream = downstream;
            this.parent = parent;
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                parent.remove(this);
            }
        }

        public void onNext(T value) {
            if (requested.get() != 0L) {
                downstream.onNext(value);
                if (requested.get() != Long.MAX_VALUE) {
                    requested.decrementAndGet();
                }
                return;
            }
            parent.remove(this);
            downstream.onError(new IllegalStateException("Can't deliver item due to lack of requests"));
        }

        public void onError(Throwable e) {
            downstream.onError(e);
        }

        public void onComplete() {
            downstream.onComplete();
        }

    }
}
