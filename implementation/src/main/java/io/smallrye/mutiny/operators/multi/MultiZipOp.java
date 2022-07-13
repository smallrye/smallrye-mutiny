package io.smallrye.mutiny.operators.multi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public final class MultiZipOp<O> extends AbstractMulti<O> {

    private final List<Publisher<?>> upstreams;
    private final Function<List<?>, ? extends O> combinator;
    private final int bufferSize;
    private final boolean collectFailures;

    public MultiZipOp(Iterable<? extends Publisher<?>> upstreams,
            Function<List<?>, ? extends O> combinator,
            int bufferSize,
            boolean collectFailures) {
        this.upstreams = new LinkedList<>();
        upstreams.forEach(this.upstreams::add);
        this.combinator = combinator;
        this.bufferSize = bufferSize;
        this.collectFailures = collectFailures;
    }

    @Override
    public void subscribe(MultiSubscriber<? super O> downstream) {
        if (upstreams.size() == 0) {
            Subscriptions.complete(downstream);
            return;
        }
        ZipCoordinator<O> coordinator = new ZipCoordinator<>(downstream, combinator, upstreams.size(), bufferSize,
                collectFailures);
        downstream.onSubscribe(coordinator);
        coordinator.subscribe(upstreams);
    }

    static final class ZipCoordinator<R> implements Flow.Subscription {

        private final AtomicInteger wip = new AtomicInteger();
        private final MultiSubscriber<? super R> downstream;
        private final List<ZipSubscriber<R>> subscribers;

        private final Function<List<?>, ? extends R> combinator;
        private final AtomicLong requested = new AtomicLong();
        private final AtomicReference<Throwable> failures = new AtomicReference<>();
        private final boolean collectFailures;

        private volatile boolean cancelled;
        private final List<Object> current;

        ZipCoordinator(MultiSubscriber<? super R> downstream,
                Function<List<?>, ? extends R> combinator, int n, int prefetch, boolean collectFailures) {
            this.downstream = downstream;
            this.combinator = combinator;
            this.collectFailures = collectFailures;

            Context context;
            if (downstream instanceof ContextSupport) {
                context = ((ContextSupport) downstream).context();
            } else {
                context = Context.empty();
            }

            subscribers = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                subscribers.add(new ZipSubscriber<>(context, this, prefetch));
            }
            this.current = new FixedSizeArrayList<>(n);
        }

        void subscribe(List<Publisher<?>> sources) {
            for (int i = 0; i < sources.size(); i++) {
                if (cancelled || (!collectFailures && failures.get() != null)) {
                    return;
                }
                Publisher<?> publisher = sources.get(i);
                publisher.subscribe(Infrastructure.onMultiSubscription(publisher, subscribers.get(i)));
            }
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                cancelAll();
            }
        }

        void error(ZipSubscriber<R> inner, Throwable e) {
            if (Subscriptions.addFailure(failures, e)) {
                inner.done = true;
                drain();
            }
        }

        void cancelAll() {
            for (ZipSubscriber<R> s : subscribers) {
                s.cancel();
            }
        }

        private void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            final List<ZipSubscriber<R>> qs = subscribers;
            final int n = qs.size();
            List<Object> values = current;

            int missed = 1;

            for (;;) {
                long requests = requested.get();
                long emitted = 0L;
                while (requests != emitted) {
                    if (cancelled) {
                        return;
                    }
                    if (!collectFailures && failures.get() != null) {
                        cancelAll();
                        Subscriptions.terminateAndPropagate(failures, downstream);
                        return;
                    }

                    boolean empty = false;

                    for (int j = 0; j < n; j++) {
                        ZipSubscriber<R> inner = qs.get(j);
                        if (values.get(j) == null) {
                            boolean d = inner.done;
                            Queue<Object> q = inner.queue;

                            Object v = q != null ? q.poll() : null;

                            boolean sourceEmpty = v == null;
                            if (d && sourceEmpty) {
                                cancelAll();
                                Subscriptions.terminateAndPropagate(failures, downstream);
                                return;
                            }
                            if (!sourceEmpty) {
                                values.set(j, v);
                            } else {
                                empty = true;
                            }
                        }
                    }

                    if (empty) {
                        break;
                    }

                    R v;

                    try {
                        v = combinator.apply(values);

                        if (v == null) {
                            throw new NullPointerException("The zipper method returned `null`");
                        }
                    } catch (Throwable ex) {
                        cancelAll();
                        Subscriptions.addFailure(failures, ex);
                        Subscriptions.terminateAndPropagate(failures, downstream);
                        return;
                    }

                    downstream.onItem(v);
                    emitted++;
                    values.clear();
                }

                if (requests == emitted) {
                    if (cancelled) {
                        return;
                    }

                    if (!collectFailures && failures.get() != null) {
                        cancelAll();
                        Subscriptions.terminateAndPropagate(failures, downstream);
                        return;
                    }

                    for (int j = 0; j < n; j++) {
                        ZipSubscriber<R> inner = qs.get(j);
                        if (values.get(j) == null) {
                            boolean d = inner.done;
                            Queue<Object> q = inner.queue;
                            Object v = q != null ? q.poll() : null;

                            boolean empty = v == null;
                            if (d && empty) {
                                cancelAll();
                                Subscriptions.terminateAndPropagate(failures, downstream);
                                return;
                            }
                            if (!empty) {
                                values.set(j, v);
                            }
                        }
                    }

                }

                if (emitted != 0L) {

                    for (ZipSubscriber<R> inner : qs) {
                        inner.request(emitted);
                    }

                    if (requests != Long.MAX_VALUE) {
                        requested.addAndGet(-emitted);
                    }
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

    static final class ZipSubscriber<R> implements MultiSubscriber<Object>, Flow.Subscription, ContextSupport {

        private final AtomicReference<Flow.Subscription> upstream = new AtomicReference<>();
        private final ZipCoordinator<R> parent;
        private final int prefetch;
        private final int limit;
        private final Context context;
        private Queue<Object> queue;
        private long produced;
        private volatile boolean done;

        ZipSubscriber(Context context, ZipCoordinator<R> parent, int prefetch) {
            this.context = context;
            this.parent = parent;
            this.prefetch = prefetch;
            this.limit = prefetch - (prefetch >> 2);
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {
            if (upstream.compareAndSet(null, s)) {
                queue = Queues.get(prefetch).get();
                s.request(prefetch);
            }
        }

        @Override
        public void onItem(Object item) {
            queue.offer(item);
            parent.drain();
        }

        @Override
        public void onFailure(Throwable t) {
            parent.error(this, t);
        }

        @Override
        public void onCompletion() {
            done = true;
            parent.drain();
        }

        @Override
        public void cancel() {
            Subscriptions.cancel(upstream);
        }

        @Override
        public void request(long n) {
            long p = produced + n;
            if (p >= limit) {
                produced = 0L;
                upstream.get().request(p);
            } else {
                produced = p;
            }
        }

        @Override
        public Context context() {
            return context;
        }
    }

    private static class FixedSizeArrayList<T> extends ArrayList<T> {

        private final int size;

        public FixedSizeArrayList(int size) {
            super(size);
            this.size = size;
            for (int i = 0; i < size; i++) {
                add(null);
            }
        }

        private void fill(int size) {
            for (int i = 0; i < size; i++) {
                set(i, null);
            }
        }

        @Override
        public void clear() {
            fill(size);
        }

        @Override
        public int size() {
            return size;
        }
    }
}
