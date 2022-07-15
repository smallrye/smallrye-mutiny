package io.smallrye.mutiny.operators.multi;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.*;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public final class MultiFlatMapOp<I, O> extends AbstractMultiOperator<I, O> {
    private final Function<? super I, ? extends Flow.Publisher<? extends O>> mapper;

    private final boolean postponeFailurePropagation;
    private final int maxConcurrency;
    private final int requests;

    public MultiFlatMapOp(Multi<? extends I> upstream,
            Function<? super I, ? extends Flow.Publisher<? extends O>> mapper,
            boolean postponeFailurePropagation,
            int maxConcurrency,
            int requests) {
        super(upstream);
        this.mapper = ParameterValidation.nonNull(mapper, "mapper");
        this.postponeFailurePropagation = postponeFailurePropagation;
        this.maxConcurrency = ParameterValidation.positive(maxConcurrency, "maxConcurrency");
        this.requests = ParameterValidation.positive(requests, "requests");
    }

    @Override
    public void subscribe(MultiSubscriber<? super O> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        FlatMapMainSubscriber<I, O> sub = new FlatMapMainSubscriber<>(subscriber,
                mapper,
                postponeFailurePropagation,
                maxConcurrency,
                requests);

        upstream.subscribe(Infrastructure.onMultiSubscription(upstream, sub));
    }

    public static final class FlatMapMainSubscriber<I, O> extends FlatMapManager<FlatMapInner<O>>
            implements MultiSubscriber<I>, Subscription, ContextSupport {

        final boolean delayError;
        final int maxConcurrency;
        final int requests;
        final int limit;
        final Function<? super I, ? extends Flow.Publisher<? extends O>> mapper;
        final Supplier<? extends Queue<O>> innerQueueSupplier;
        final MultiSubscriber<? super O> downstream;

        final AtomicReference<Throwable> failures = new AtomicReference<>();

        volatile boolean done;
        volatile boolean cancelled;

        volatile Subscription upstream = null;
        private static final AtomicReferenceFieldUpdater<FlatMapMainSubscriber, Subscription> UPSTREAM_UPDATER = AtomicReferenceFieldUpdater
                .newUpdater(FlatMapMainSubscriber.class, Subscription.class, "upstream");

        AtomicLong requested = new AtomicLong();

        AtomicInteger wip = new AtomicInteger();

        @SuppressWarnings("rawtypes")
        static final FlatMapInner[] EMPTY_INNER_ARRAY = new FlatMapInner[0];

        @SuppressWarnings("rawtypes")
        static final FlatMapInner[] TERMINATED_INNER_ARRAY = new FlatMapInner[0];

        int lastIndex;

        public FlatMapMainSubscriber(MultiSubscriber<? super O> downstream,
                Function<? super I, ? extends Flow.Publisher<? extends O>> mapper,
                boolean delayError,
                int concurrency,
                int requests) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.delayError = delayError;
            this.maxConcurrency = concurrency;
            this.requests = requests;
            this.innerQueueSupplier = requests == 0 ? Queues.getXsQueueSupplier() : Queues.get(requests);
            this.limit = Subscriptions.unboundedOrLimit(concurrency);
        }

        @SuppressWarnings("unchecked")
        @Override
        FlatMapInner<O>[] empty() {
            return EMPTY_INNER_ARRAY;
        }

        @SuppressWarnings("unchecked")
        @Override
        FlatMapInner<O>[] terminated() {
            return TERMINATED_INNER_ARRAY;
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
        void unsubscribeEntry(FlatMapInner<O> entry, boolean fromOnError) {
            entry.cancel(fromOnError);
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
                drain();
            } else {
                downstream.onFailure(new IllegalArgumentException("Invalid requests, must be greater than 0"));
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;

                if (wip.getAndIncrement() == 0) {
                    UPSTREAM_UPDATER.getAndSet(this, Subscriptions.CANCELLED).cancel();
                    unsubscribe();
                }
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (UPSTREAM_UPDATER.compareAndSet(this, null, s)) {
                downstream.onSubscribe(this);
                s.request(Subscriptions.unboundedOrRequests(maxConcurrency));
            }
        }

        @Override
        public void onItem(I item) {
            if (done) {
                return;
            }

            Flow.Publisher<? extends O> p;

            try {
                p = mapper.apply(item);
                if (p == null) {
                    throw new NullPointerException(ParameterValidation.MAPPER_RETURNED_NULL);
                }
            } catch (Throwable e) {
                cancelled = true;
                done = true;
                Subscriptions.addFailure(failures, e);
                cancelUpstream(false);
                handleTerminationIfDone();
                return;
            }

            FlatMapInner<O> inner = new FlatMapInner<>(this, requests);
            if (add(inner)) {
                p.subscribe(inner);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (done) {
                Infrastructure.handleDroppedException(failure);
                return;
            }
            Subscriptions.addFailure(failures, failure);
            done = true;
            if (!delayError) {
                for (FlatMapInner<O> inner : inners.getAndSet(terminated())) {
                    if (inner != null) {
                        inner.cancel(false);
                    }
                }
            }
            drain();
        }

        @Override
        public void onCompletion() {
            if (done) {
                return;
            }

            done = true;
            drain();
        }

        void tryEmit(FlatMapInner<O> inner, O item) {
            if (wip.compareAndSet(0, 1)) {
                long req = requested.get();
                Queue<O> q = inner.queue;
                if (req != 0 && (q == null || q.isEmpty())) {
                    downstream.onNext(item);

                    if (req != Long.MAX_VALUE) {
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

            final MultiSubscriber<? super O> a = downstream;

            for (;;) {

                boolean d;

                FlatMapInner<O>[] as = get();

                int n = as.length;

                boolean noSources = isEmpty();

                if (ifDoneOrCancelled()) {
                    return;
                }

                boolean again = false;

                long r = requested.get();
                long e = 0L;
                long replenishMain = 0L;

                if (r != 0L && !noSources) {

                    int j = lastIndex;
                    for (int i = 0; i < n; i++) {
                        if (cancelled) {
                            cancelUpstream(false);
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
                                        Subscriptions.addFailure(failures, ex);
                                        v = null;
                                        d = true;
                                    }

                                    boolean empty = v == null;

                                    if (ifDoneOrCancelled()) {
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

                                    a.onItem(v);

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
                            cancelUpstream(false);
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
                    upstream.request(replenishMain);
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

        private void cancelUpstream(boolean fromOnError) {
            Subscription subscription = UPSTREAM_UPDATER.getAndSet(this, Subscriptions.CANCELLED);
            if (subscription != null) {
                subscription.cancel();
            }
            unsubscribe(fromOnError);
        }

        boolean ifDoneOrCancelled() {
            if (cancelled) {
                cancelUpstream(false);
                return true;
            }

            return handleTerminationIfDone();

        }

        private boolean handleTerminationIfDone() {
            boolean wasDone = done;
            boolean isEmpty = isEmpty();
            if (delayError) {
                if (wasDone && isEmpty) {
                    Throwable e = failures.get();
                    if (e != null && e != Subscriptions.TERMINATED) {
                        Throwable throwable = failures.getAndSet(Subscriptions.TERMINATED);
                        downstream.onFailure(throwable);
                    } else {
                        downstream.onCompletion();
                    }
                    return true;
                }
            } else {
                if (wasDone) {
                    Throwable e = failures.get();
                    if (e != null && e != Subscriptions.TERMINATED) {
                        Throwable throwable = failures.getAndSet(Subscriptions.TERMINATED);
                        unsubscribe(true);
                        downstream.onFailure(throwable);
                        return true;
                    } else if (isEmpty) {
                        downstream.onCompletion();
                        return true;
                    }
                }
            }
            return false;
        }

        void innerError(FlatMapInner<O> inner, Throwable fail) {
            if (fail != null) {
                if (Subscriptions.addFailure(failures, fail)) {
                    inner.done = true;
                    if (!delayError) {
                        cancelUpstream(true);
                        downstream.onFailure(fail);
                        return;
                    }
                    drain();
                }
            } else {
                drain();
            }
        }

        void failOverflow() {
            Throwable e = new BackPressureFailure("Buffer full, cannot emit item");
            Subscriptions.addFailure(failures, e);
        }

        void innerComplete() {
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

        @Override
        public Context context() {
            if (downstream instanceof ContextSupport) {
                return ((ContextSupport) downstream).context();
            } else {
                return Context.empty();
            }
        }
    }

    static final class FlatMapInner<O> implements Subscription, MultiSubscriber<O>, ContextSupport {

        final FlatMapMainSubscriber<?, O> parent;

        final int requests;

        final int limit;

        volatile Subscription subscription = null;
        private static final AtomicReferenceFieldUpdater<FlatMapInner, Subscription> SUBSCRIPTION_UPDATER = AtomicReferenceFieldUpdater
                .newUpdater(FlatMapInner.class, Subscription.class, "subscription");

        long produced;

        volatile Queue<O> queue;

        volatile boolean done;

        int index;

        FlatMapInner(FlatMapMainSubscriber<?, O> parent, int requests) {
            this.parent = parent;
            this.requests = requests;
            this.limit = Subscriptions.unboundedOrLimit(requests);
        }

        @Override
        public void onSubscribe(Subscription s) {
            Objects.requireNonNull(s);
            if (SUBSCRIPTION_UPDATER.compareAndSet(this, null, s)) {
                s.request(Subscriptions.unboundedOrRequests(requests));
            }
        }

        @Override
        public void onItem(O item) {
            parent.tryEmit(this, item);
        }

        @Override
        public void onFailure(Throwable failure) {
            Objects.requireNonNull(failure);
            done = true;
            parent.innerError(this, failure);
        }

        @Override
        public void onCompletion() {
            done = true;
            parent.innerComplete();
        }

        @Override
        public void request(long n) {
            long p = produced + n;
            if (p >= limit) {
                produced = 0L;
                subscription.request(p);
            } else {
                produced = p;
            }
        }

        @Override
        public void cancel() {
            cancel(true);
        }

        public void cancel(boolean doNotCancel) {
            if (!doNotCancel) {
                Subscription last = SUBSCRIPTION_UPDATER.getAndSet(this, Subscriptions.CANCELLED);
                if (last != null) {
                    last.cancel();
                }
            }
            if (queue != null) {
                queue.clear();
                queue = null;
            }
        }

        @Override
        public Context context() {
            return parent.context();
        }
    }
}
