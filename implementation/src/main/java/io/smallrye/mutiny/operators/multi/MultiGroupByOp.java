package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.GroupedMulti;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.SpscLinkedArrayQueue;
import io.smallrye.mutiny.operators.AbstractMulti;

public final class MultiGroupByOp<T, K, V> extends AbstractMultiOperator<T, GroupedMulti<K, V>> {
    private final Function<? super T, ? extends K> keySelector;
    private final Function<? super T, ? extends V> valueSelector;

    public MultiGroupByOp(Multi<T> upstream,
            Function<? super T, ? extends K> keySelector,
            Function<? super T, ? extends V> valueSelector) {
        super(upstream);
        this.keySelector = keySelector;
        this.valueSelector = valueSelector;
    }

    @Override
    public void subscribe(Subscriber<? super GroupedMulti<K, V>> downstream) {
        if (downstream == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        final Map<Object, GroupedUnicast<K, V>> groups = new ConcurrentHashMap<>();
        MultiGroupByProcessor<T, K, V> processor = new MultiGroupByProcessor<>(downstream, keySelector, valueSelector,
                groups);
        upstream.subscribe(processor);
    }

    public static final class MultiGroupByProcessor<T, K, V> extends MultiOperatorProcessor<T, GroupedMulti<K, V>> {
        private final Function<? super T, ? extends K> keySelector;
        private final Function<? super T, ? extends V> valueSelector;
        private final Map<Object, GroupedUnicast<K, V>> groups;
        private final SpscLinkedArrayQueue<GroupedMulti<K, V>> queue;

        private static final Object NO_KEY = new Object();

        private final AtomicBoolean cancelled = new AtomicBoolean();

        private final AtomicLong requested = new AtomicLong();

        private final AtomicInteger groupCount = new AtomicInteger(1);
        private final AtomicInteger wip = new AtomicInteger();

        Throwable failure;
        volatile boolean finished;
        boolean done;

        public MultiGroupByProcessor(Subscriber<? super GroupedMulti<K, V>> downstream,
                Function<? super T, ? extends K> keySelector,
                Function<? super T, ? extends V> valueSelector,
                Map<Object, GroupedUnicast<K, V>> groups) {
            super(downstream);
            this.keySelector = keySelector;
            this.valueSelector = valueSelector;
            this.groups = groups;
            this.queue = new SpscLinkedArrayQueue<>(128);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                // Propagate subscription to downstream.
                downstream.onSubscribe(this);
                subscription.request(128);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onNext(T item) {
            if (isDone()) {
                return;
            }

            K key;
            try {
                key = keySelector.apply(item);
            } catch (Throwable ex) {
                super.cancel();
                super.onError(ex);
                return;
            }

            boolean newGroup = false;
            Object mapKey = key != null ? key : NO_KEY;
            GroupedUnicast<K, V> group = groups.get(mapKey);
            if (group == null) {
                if (isCancelled()) {
                    return;
                }

                group = GroupedUnicast.createWith(key, this);
                groups.put(mapKey, group);
                groupCount.getAndIncrement();
                newGroup = true;
            }

            V value;
            try {
                value = valueSelector.apply(item);
                if (value == null) {
                    throw new NullPointerException("The selector returned `null`");
                }
            } catch (Throwable ex) {
                super.cancel();
                super.onError(ex);
                return;
            }

            group.onNext(value);
            if (newGroup) {
                this.queue.offer(group);
                drain();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            Subscription subscription = upstream.getAndSet(CANCELLED);
            if (subscription != CANCELLED) {
                done = true;
                groups.values().forEach(group -> group.onError(throwable));
                groups.clear();
                failure = throwable;
                finished = true;
                drain();
            }
        }

        @Override
        public void onComplete() {
            Subscription subscription = upstream.getAndSet(CANCELLED);
            if (subscription != CANCELLED) {
                done = true;
                groups.values().forEach(GroupedUnicast::onComplete);
                groups.clear();
                finished = true;
                drain();
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
            // cancelling the main source means we don't want any more groups
            // but running groups still require new values
            if (cancelled.compareAndSet(false, true)) {
                if (groupCount.decrementAndGet() == 0) {
                    Subscriptions.cancel(upstream);
                }
            }
        }

        public void cancel(K key) {
            Object mapKey = key != null ? key : NO_KEY;
            groups.remove(mapKey);
            if (groupCount.decrementAndGet() == 0) {
                Subscriptions.cancel(upstream);

                if (wip.getAndIncrement() == 0) {
                    queue.clear();
                }
            }
        }

        private void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            int missed = 1;

            final SpscLinkedArrayQueue<GroupedMulti<K, V>> q = this.queue;

            for (;;) {

                long requests = requested.get();
                long emitted = 0L;

                while (emitted != requests) {
                    boolean isDone = finished;

                    GroupedMulti<K, V> t = q.poll();

                    boolean hasNoMoreGroup = t == null;
                    if (isDoneOrCancelled(isDone, hasNoMoreGroup, q)) {
                        return;
                    }
                    if (hasNoMoreGroup) {
                        break;
                    }
                    this.downstream.onNext(t);
                    emitted++;
                }

                if (emitted == requests && isDoneOrCancelled(finished, q.isEmpty(), q)) {
                    return;
                }

                if (emitted != 0L) {
                    if (requests != Long.MAX_VALUE) {
                        requested.addAndGet(-emitted);
                    }
                    super.request(emitted);
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        boolean isDoneOrCancelled(boolean d, boolean empty, SpscLinkedArrayQueue<?> q) {
            if (isCancelled()) {
                q.clear();
                return true;
            }

            if (d) {
                Throwable ex = failure;
                if (ex != null) {
                    q.clear();
                    downstream.onError(ex);
                    return true;
                } else if (empty) {
                    downstream.onComplete();
                    return true;
                }
            }

            return false;
        }
    }

    static final class GroupedUnicast<K, T> extends AbstractMulti<T> implements GroupedMulti<K, T> {

        private final State<T, K> downstream;
        private final K key;

        public static <T, K> GroupedUnicast<K, T> createWith(K key,
                MultiGroupByProcessor<?, K, T> parent) {
            State<T, K> state = new State<>(parent, key);
            return new GroupedUnicast<>(key, state);
        }

        protected GroupedUnicast(K key, State<T, K> downstream) {
            this.key = key;
            this.downstream = downstream;
        }

        @Override
        public void subscribe(Subscriber<? super T> s) {
            downstream.subscribe(s);
        }

        public void onNext(T t) {
            downstream.onNext(t);
        }

        public void onError(Throwable e) {
            downstream.onError(e);
        }

        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public K key() {
            return key;
        }
    }

    @SuppressWarnings("PublisherImplementation")
    private static final class State<T, K> implements Subscription, Publisher<T> {

        private final AtomicReference<Subscriber<? super T>> downstream = new AtomicReference<>();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicLong requested = new AtomicLong();
        private final AtomicBoolean done = new AtomicBoolean();
        private final AtomicInteger wip = new AtomicInteger();

        private final K key;
        private final SpscLinkedArrayQueue<T> queue;
        private final MultiGroupByProcessor<?, K, T> parent;

        private Throwable failure;

        State(MultiGroupByProcessor<?, K, T> parent, K key) {
            this.parent = parent;
            this.queue = new SpscLinkedArrayQueue<>(128);
            this.key = key;
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
            if (cancelled.compareAndSet(false, true)) {
                parent.cancel(key);
            }
        }

        @Override
        public void subscribe(Subscriber<? super T> s) {
            if (downstream.compareAndSet(null, s)) {
                s.onSubscribe(this);
                drain();
            } else {
                Subscriptions.fail(s, new IllegalStateException("only 1 subscriber allowed"));
            }
        }

        public void onNext(T t) {
            if (!done.get()) {
                queue.offer(t);
                drain();
            }
        }

        public void onError(Throwable e) {
            if (done.compareAndSet(false, true)) {
                failure = e;
                drain();
            }
        }

        public void onComplete() {
            if (done.compareAndSet(false, true)) {
                drain();
            }
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            final SpscLinkedArrayQueue<T> q = queue;
            Subscriber<? super T> actual = downstream.get();
            for (;;) {
                if (actual != null) {
                    long r = requested.get();
                    long e = 0;

                    while (e != r) {
                        boolean isDone = done.get();
                        T v = q.poll();
                        boolean empty = v == null;

                        if (hasCompleted(isDone, empty)) {
                            return;
                        }

                        if (empty) {
                            break;
                        }

                        actual.onNext(v);

                        e++;
                    }

                    if (e == r && hasCompleted(done.get(), q.isEmpty())) {
                        return;
                    }

                    if (e != 0L) {
                        if (r != Long.MAX_VALUE) {
                            requested.addAndGet(-e);
                        }
                        parent.upstream.get().request(e);
                    }
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
                if (actual == null) {
                    actual = downstream.get();
                }
            }
        }

        boolean hasCompleted(boolean isDone, boolean isEmpty) {
            if (cancelled.get()) {
                queue.clear();
                return true;
            }

            if (isDone) {
                Throwable e = failure;
                if (e != null) {
                    queue.clear();
                    downstream.get().onError(e);
                    return true;
                } else if (isEmpty) {
                    downstream.get().onComplete();
                    return true;
                }
            }
            return false;
        }
    }
}
