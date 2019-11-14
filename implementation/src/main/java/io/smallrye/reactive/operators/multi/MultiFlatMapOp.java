package io.smallrye.reactive.operators.multi;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.CompositeException;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.subscription.BackPressureFailure;

public final class MultiFlatMapOp<I, O> extends AbstractMultiOperator<I, O> {
    private final Function<? super I, ? extends Publisher<? extends O>> mapper;

    private final boolean postponeFailurePropagation;
    private final int maxConcurrency;
    private final int prefetch;

    private final Supplier<? extends Queue<O>> mainQueueSupplier;
    private final Supplier<? extends Queue<O>> innerQueueSupplier;

    public MultiFlatMapOp(Multi<? extends I> upstream,
            Function<? super I, ? extends Publisher<? extends O>> mapper,
            boolean postponeFailurePropagation,
            int maxConcurrency,
            int prefetch, Supplier<? extends Queue<O>> mainQueueSupplier,
            Supplier<? extends Queue<O>> innerQueueSupplier) {
        super(upstream);
        this.mapper = ParameterValidation.nonNull(mapper, "mapper");
        this.postponeFailurePropagation = postponeFailurePropagation;
        this.prefetch = ParameterValidation.positive(prefetch, "prefetch");
        this.maxConcurrency = ParameterValidation.positive(maxConcurrency, "maxConcurrency");
        this.mainQueueSupplier = ParameterValidation.nonNull(mainQueueSupplier, "mainQueueSupplier");
        this.innerQueueSupplier = ParameterValidation.nonNull(innerQueueSupplier, "innerQueueSupplier");
    }

    @Override
    protected Publisher<O> publisher() {
        return this;
    }

    @Override
    public void subscribe(Subscriber<? super O> subscriber) {
        FlatMapMainSubscriber<I, O> sub = new FlatMapMainSubscriber<>(subscriber,
                mapper,
                postponeFailurePropagation,
                maxConcurrency,
                prefetch, mainQueueSupplier,
                innerQueueSupplier);
        upstream.subscribe(sub);
    }

    public static final class FlatMapMainSubscriber<I, O> extends FlatMapManager<FlatMapInner<O>>
            implements Subscriber<I>, Subscription {

        final boolean delayError;
        final int maxConcurrency;
        final int limit;
        final Function<? super I, ? extends Publisher<? extends O>> mapper;
        final Supplier<? extends Queue<O>> mainQueueSupplier;
        final Supplier<? extends Queue<O>> innerQueueSupplier;
        final Subscriber<? super O> actual;
        private final int prefetch;

        volatile Queue<O> queue;

        AtomicReference<Throwable> failures = new AtomicReference<>();

        volatile boolean done;
        volatile boolean cancelled;

        AtomicReference<Subscription> subscription = new AtomicReference<>();

        AtomicLong requested = new AtomicLong();

        AtomicInteger wip = new AtomicInteger();

        @SuppressWarnings("rawtypes")
        static final FlatMapInner[] EMPTY = new FlatMapInner[0];

        @SuppressWarnings("rawtypes")
        static final FlatMapInner[] TERMINATED = new FlatMapInner[0];

        int lastIndex;

        FlatMapMainSubscriber(Subscriber<? super O> actual,
                Function<? super I, ? extends Publisher<? extends O>> mapper,
                boolean delayError,
                int maxConcurrency,
                int prefetch, Supplier<? extends Queue<O>> mainQueueSupplier,
                Supplier<? extends Queue<O>> innerQueueSupplier) {
            this.actual = actual;
            this.mapper = mapper;
            this.delayError = delayError;
            this.maxConcurrency = maxConcurrency;
            this.mainQueueSupplier = mainQueueSupplier;
            this.innerQueueSupplier = innerQueueSupplier;
            this.prefetch = prefetch;
            this.limit = Subscriptions.unboundedOrLimit(maxConcurrency);
        }

        @SuppressWarnings("unchecked")
        @Override
        FlatMapInner<O>[] empty() {
            return EMPTY;
        }

        @SuppressWarnings("unchecked")
        @Override
        FlatMapInner<O>[] terminated() {
            return TERMINATED;
        }

        @SuppressWarnings("unchecked")
        @Override
        FlatMapInner<O>[] newArray(int size) {
            return new FlatMapInner[size];
        }

        @Override
        void setIndex(FlatMapInner<O> entry, int index) {
            entry.index = index;
        }

        @Override
        void unsubscribeEntry(FlatMapInner<O> entry) {
            entry.cancel();
        }

        @Override
        public void request(long n) {
            if (Subscriptions.validate(n)) {
                Subscriptions.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;

                if (wip.getAndIncrement() == 0) {
                    queue.clear();
                    queue = null;
                    subscription.getAndSet(Subscriptions.CANCELLED).cancel();
                    unsubscribe();
                }
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (subscription.compareAndSet(null, s)) {
                actual.onSubscribe(this);
                s.request(Subscriptions.unboundedOrMaxConcurrency(maxConcurrency));
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onNext(I item) {
            if (done) {
                return;
            }

            Publisher<? extends O> p;

            try {
                p = mapper.apply(item);
                if (p == null) {
                    throw new NullPointerException(ParameterValidation.MAPPER_RETURNED_NULL);
                }
            } catch (Throwable e) {
                onError(e);
                return;
            }

            FlatMapInner<O> inner = new FlatMapInner<>(this, maxConcurrency);
            if (add(inner)) {
                p.subscribe(inner);
            }
        }

        Queue<O> getOrCreateScalarQueue() {
            Queue<O> q = queue;
            if (q == null) {
                q = mainQueueSupplier.get();
                queue = q;
            }
            return q;
        }

        private boolean addFailure(Throwable failure) {
            Throwable current = failures.get();

            if (current == Subscriptions.TERMINATED) {
                return false;
            }

            if (current instanceof CompositeException) {
                failures.set(new CompositeException((CompositeException) current, failure));
                return true;
            }

            if (current == null) {
                failures.set(failure);
            } else {
                failures.set(new CompositeException(current, failure));
            }

            return true;
        }

        @Override
        public void onError(Throwable failure) {
            if (done) {
                return;
            }
            addFailure(failure);
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }

            done = true;
            drain();
        }

        void tryEmit(FlatMapInner<O> inner, O item) {
            if (wip.compareAndSet(0, 1)) {
                long r = requested.get();

                Queue<O> q = inner.queue;
                if (r != 0 && (q == null || q.isEmpty())) {
                    actual.onNext(item);

                    if (r != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }

                    inner.request(1);
                } else {
                    if (q == null) {
                        q = getOrCreateInnerQueue(inner);
                    }

                    if (!q.offer(item)) {
                        failOverflow();
                        inner.done = true;
                        drainLoop();
                        return;
                    }
                }
                if (wip.decrementAndGet() == 0) {
                    return;
                }

                drainLoop();
            } else {
                Queue<O> q = getOrCreateInnerQueue(inner);

                if (!q.offer(item)) {
                    failOverflow();
                    inner.done = true;
                }
                drain();
            }
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            drainLoop();
        }

        void drainLoop() {
            int missed = 1;

            final Subscriber<? super O> a = actual;

            for (;;) {

                boolean d = done;

                FlatMapInner<O>[] as = get();

                int n = as.length;

                Queue<O> sq = queue;

                boolean noSources = isEmpty();

                if (checkTerminated(d, noSources && (sq == null || sq.isEmpty()), a)) {
                    return;
                }

                boolean again = false;

                long r = requested.get();
                long e = 0L;
                long replenishMain = 0L;

                if (r != 0L && sq != null) {

                    while (e != r) {
                        d = done;

                        O v = sq.poll();

                        boolean empty = v == null;

                        if (checkTerminated(d, false, a)) {
                            return;
                        }

                        if (empty) {
                            break;
                        }

                        a.onNext(v);

                        e++;
                    }

                    if (e != 0L) {
                        replenishMain += e;
                        if (r != Long.MAX_VALUE) {
                            r = requested.addAndGet(-e);
                        }
                        e = 0L;
                        again = true;
                    }
                }
                if (r != 0L && !noSources) {

                    int j = lastIndex;
                    for (int i = 0; i < n; i++) {
                        if (cancelled) {
                            queue.clear();
                            queue = null;
                            subscription.getAndSet(Subscriptions.CANCELLED).cancel();
                            unsubscribe();
                            return;
                        }

                        FlatMapInner<O> inner = as[j];
                        if (inner != null) {
                            d = inner.done;
                            Queue<O> q = inner.queue;
                            if (d && q == null) {
                                remove(inner.index);
                                again = true;
                                replenishMain++;
                            } else if (q != null) {
                                while (e != r) {
                                    d = inner.done;

                                    O v;

                                    try {
                                        v = q.poll();
                                    } catch (Throwable ex) {
                                        addFailure(ex);
                                        v = null;
                                        d = true;
                                    }

                                    boolean empty = v == null;

                                    if (checkTerminated(d, false, a)) {
                                        return;
                                    }

                                    if (d && empty) {
                                        remove(inner.index);
                                        again = true;
                                        replenishMain++;
                                        break;
                                    }

                                    if (empty) {
                                        break;
                                    }

                                    a.onNext(v);

                                    e++;
                                }

                                if (e == r) {
                                    d = inner.done;
                                    boolean empty = q.isEmpty();
                                    if (d && empty) {
                                        remove(inner.index);
                                        again = true;
                                        replenishMain++;
                                    }
                                }

                                if (e != 0L) {
                                    if (!inner.done) {
                                        inner.request(e);
                                    }
                                    if (r != Long.MAX_VALUE) {
                                        r = requested.addAndGet(-e);
                                        if (r == 0L) {
                                            break; // 0 .. numberOfItems - 1
                                        }
                                    }
                                    e = 0L;
                                }
                            }
                        }

                        if (r == 0L) {
                            break;
                        }

                        if (++j == n) {
                            j = 0;
                        }
                    }

                    lastIndex = j;
                }

                if (r == 0L && !noSources) {
                    as = get();
                    n = as.length;

                    for (int i = 0; i < n; i++) {
                        if (cancelled) {
                            queue.clear();
                            queue = null;
                            subscription.getAndSet(Subscriptions.CANCELLED).cancel();
                            unsubscribe();
                            return;
                        }

                        FlatMapInner<O> inner = as[i];
                        if (inner == null) {
                            continue;
                        }

                        d = inner.done;
                        Queue<O> q = inner.queue;
                        boolean empty = (q == null || q.isEmpty());

                        // if we have a non-empty source then quit the cleanup
                        if (!empty) {
                            break;
                        }

                        if (d && empty) {
                            remove(inner.index);
                            again = true;
                            replenishMain++;
                        }
                    }
                }

                if (replenishMain != 0L && !done && !cancelled) {
                    subscription.get().request(replenishMain);
                }

                if (again) {
                    continue;
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        boolean checkTerminated(boolean wasDone, boolean isEmpty, Subscriber<?> subscriber) {
            if (cancelled) {
                queue.clear();
                queue = null;
                subscription.getAndSet(Subscriptions.CANCELLED).cancel();
                unsubscribe();

                return true;
            }

            if (delayError) {
                if (wasDone && isEmpty) {
                    Throwable e = failures.get();
                    if (e != null && e != Subscriptions.TERMINATED) {
                        Throwable throwable = failures.getAndSet(Subscriptions.TERMINATED);
                        subscriber.onError(throwable);
                    } else {
                        subscriber.onComplete();
                    }
                    return true;
                }
            } else {
                if (wasDone) {
                    Throwable e = failures.get();
                    if (e != null && e != Subscriptions.TERMINATED) {
                        Throwable throwable = failures.getAndSet(Subscriptions.TERMINATED);
                        if (queue != null) {
                            queue.clear();
                            queue = null;
                        }
                        subscription.getAndSet(Subscriptions.CANCELLED).cancel();
                        unsubscribe();
                        subscriber.onError(throwable);
                        return true;
                    } else if (isEmpty) {
                        subscriber.onComplete();
                        return true;
                    }
                }
            }

            return false;
        }

        void innerError(FlatMapInner<O> inner, Throwable e) {
            if (e != null) {
                if (addFailure(e)) {
                    inner.done = true;
                    if (!delayError) {
                        done = true;
                    }
                    drain();
                }
            } else {
                drain();
            }
        }

        void failOverflow() {
            Throwable e = new BackPressureFailure("Buffer full, cannot emit item");
            addFailure(e);
        }

        void innerComplete(FlatMapInner<O> inner) {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            drainLoop();
        }

        Queue<O> getOrCreateInnerQueue(FlatMapInner<O> inner) {
            Queue<O> q = inner.queue;
            if (q == null) {
                q = innerQueueSupplier.get();
                inner.queue = q;
            }
            return q;
        }

    }

    static final class FlatMapInner<O> implements Subscription, Subscriber<O> {

        final FlatMapMainSubscriber<?, O> parent;

        final int prefetch;

        final int limit;

        AtomicReference<Subscription> subscription = new AtomicReference<>();

        long produced;

        volatile Queue<O> queue;

        volatile boolean done;

        /**
         * Represents the optimization mode of this inner subscriber.
         */
        int sourceMode;

        int index;

        FlatMapInner(FlatMapMainSubscriber<?, O> parent, int prefetch) {
            this.parent = parent;
            this.prefetch = prefetch;
            this.limit = Subscriptions.unboundedOrLimit(prefetch);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (subscription.compareAndSet(null, s)) {
                s.request(Subscriptions.unboundedOrMaxConcurrency(prefetch));
            }
        }

        @Override
        public void onNext(O t) {
            parent.tryEmit(this, t);
        }

        @Override
        public void onError(Throwable t) {
            done = true;
            parent.innerError(this, t);
        }

        @Override
        public void onComplete() {
            done = true;
            parent.innerComplete(this);
        }

        @Override
        public void request(long n) {
            long p = produced + n;
            if (p >= limit) {
                produced = 0L;
                subscription.get().request(p);
            } else {
                produced = p;
            }
        }

        @Override
        public void cancel() {
            subscription.getAndSet(Subscriptions.CANCELLED).cancel();
            if (queue != null) {
                queue.clear();
                queue = null;
            }
        }
    }
}
