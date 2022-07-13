package io.smallrye.mutiny.operators.multi;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * A {@code multi} caching the events emitted from upstreams and replaying it to subscribers.
 * This multi can have several subscribers.
 *
 * @param <T> the type of item
 */
@SuppressWarnings("SubscriberImplementation")
public class MultiCacheOp<T> extends AbstractMultiOperator<T, T> implements Subscriber<T>, ContextSupport {

    /**
     * Stores whether we already subscribed to the upstream.
     */
    private final AtomicBoolean hasSubscribedToUpstream = new AtomicBoolean();

    /**
     * The current set of downstream subscribers.
     */
    private final List<CacheSubscription<T>> subscribers = new CopyOnWriteArrayList<>();
    private volatile boolean terminated;

    private final CopyOnWriteArrayList<Node<T>> history = new CopyOnWriteArrayList<>();

    private volatile Context context;

    /**
     * If the upstream has terminated with a failure, this stores the failure.
     */
    private Throwable failure;

    /**
     * {@code true} if the source has terminated.
     */
    private volatile boolean done;

    public MultiCacheOp(Multi<T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        CacheSubscription<T> consumer = new CacheSubscription<>(downstream, this);
        downstream.onSubscribe(consumer);
        addDownstreamSubscription(consumer);

        if (hasSubscribedToUpstream.compareAndSet(false, true)) {
            if (downstream instanceof ContextSupport) {
                this.context = ((ContextSupport) downstream).context();
            } else {
                this.context = Context.empty();
            }
            upstream.subscribe().withSubscriber(this);
        } else {
            // replay
            consumer.replay();
        }
    }

    private synchronized void addDownstreamSubscription(CacheSubscription<T> consumer) {
        if (terminated) {
            return;
        }
        subscribers.add(consumer);
    }

    private synchronized void remove(CacheSubscription<T> consumer) {
        subscribers.remove(consumer);
    }

    @Override
    public void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
    }

    @Override
    public synchronized void onNext(T item) {
        history.add(new Node<>(item));
        for (CacheSubscription<T> consumer : subscribers) {
            // replay
            consumer.replay();
        }
    }

    @Override
    public void onError(Throwable t) {
        if (done) {
            return;
        }
        failure = t;
        done = true;
        terminated = true;
        for (CacheSubscription<T> consumer : subscribers) {
            consumer.replay();
        }
    }

    @Override
    public void onComplete() {
        done = true;
        terminated = true;
        for (CacheSubscription<T> consumer : subscribers) {
            consumer.replay();
        }
    }

    @Override
    public Context context() {
        return this.context;
    }

    /**
     * Hosts the downstream consumer and its current requested and replay states.
     * {@code this} holds the work-in-progress counter for the serialized replay.
     *
     * @param <T> the value type
     */
    static final class CacheSubscription<T> implements Subscription {

        private final MultiSubscriber<? super T> downstream;
        private final MultiCacheOp<T> cache;
        private final AtomicLong requested = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();
        private int lastIndex;

        CacheSubscription(MultiSubscriber<? super T> downstream, MultiCacheOp<T> cache) {
            this.downstream = downstream;
            this.cache = cache;
            this.lastIndex = -1;
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
                replay();
            }
        }

        public void replay() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            int missed = 1;
            CopyOnWriteArrayList<Node<T>> history = cache.history;

            for (;;) {

                if (cache.done && !hasNext()) {
                    if (cache.failure != null) {
                        downstream.onError(cache.failure);
                    } else {
                        downstream.onCompletion();
                    }
                    return;
                }

                long consumerRequested = requested.get();
                if (consumerRequested == Long.MIN_VALUE) { // cancelled.
                    return;
                }

                if (consumerRequested > 0L && hasNext()) {
                    lastIndex = lastIndex + 1;
                    Node<T> node = history.get(lastIndex);
                    downstream.onItem(node.item);
                    Subscriptions.subtract(requested, 1);
                    continue;
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }

        }

        @Override
        public void cancel() {
            if (requested.getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                cache.remove(this);
            }
        }

        boolean hasNext() {
            return lastIndex < cache.history.size() - 1;
        }
    }

    /**
     * Node stored in the list.
     *
     * @param <T> the type of item.
     */
    static final class Node<T> {

        private final T item;

        Node(T item) {
            this.item = item;
        }
    }
}
