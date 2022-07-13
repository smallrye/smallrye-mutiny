package io.smallrye.mutiny.operators.multi.multicast;

import java.util.Queue;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * A connectable observable which shares an underlying source and dispatches source values to subscribers in a
 * back-pressure-aware manner.
 *
 * @param <T> the value type
 */
public final class MultiPublishOp<T> extends ConnectableMulti<T> {
    /**
     * Indicates this child has been cancelled: the state is swapped in atomically and
     * will prevent the dispatch() to emit (too many) values to a terminated child subscriber.
     */
    private static final long CANCELLED = Long.MIN_VALUE;

    /**
     * Holds the current subscriber that is, will be or just was subscribed to the source observable.
     */
    private final AtomicReference<PublishSubscriber<T>> current;

    /**
     * The size of the prefetch buffer.
     */
    private final int bufferSize;

    private final Publisher<T> onSubscribe;

    public static <T> ConnectableMulti<T> create(Multi<T> upstream) {
        final AtomicReference<PublishSubscriber<T>> curr = new AtomicReference<>();
        Publisher<T> onSubscribe = new InnerPublisher<>(curr, 128);
        return new MultiPublishOp<>(onSubscribe, upstream, curr, 128);
    }

    private MultiPublishOp(Publisher<T> onSubscribe, Multi<T> upstream,
            final AtomicReference<PublishSubscriber<T>> current, int bufferSize) {
        super(upstream);
        this.onSubscribe = onSubscribe;
        this.current = current;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> s) {
        onSubscribe.subscribe(Infrastructure.onMultiSubscription(upstream, s));
    }

    @Override
    public void connect(ConnectableMultiConnection connection) {
        boolean doConnect;
        PublishSubscriber<T> ps;
        // we loop because concurrent connect/disconnect and termination may change the state
        for (;;) {
            // retrieve the current subscriber-to-source instance
            ps = current.get();
            // if there is none yet or the current has been disposed
            if (ps == null || ps.cancelled.get()) {
                // create a new subscriber-to-source
                MultiSubscriber<?> subscriber = connection.getSubscriber();
                Context context;
                if (subscriber instanceof ContextSupport) {
                    context = ((ContextSupport) subscriber).context();
                } else {
                    context = Context.empty();
                }
                PublishSubscriber<T> u = new PublishSubscriber<>(current, bufferSize, context);
                // try setting it as the current subscriber-to-source
                if (!current.compareAndSet(ps, u)) {
                    // did not work, perhaps a new subscriber arrived
                    // and created a new subscriber-to-source as well, retry
                    continue;
                }
                ps = u;
            }
            // if connect() was called concurrently, only one of them should actually
            // connect to the source
            doConnect = !ps.shouldConnect.get() && ps.shouldConnect.compareAndSet(false, true);
            break;
        }

        if (connection != null) {
            connection.accept(ps);
        }

        if (doConnect) {
            upstream.subscribe(Infrastructure.onMultiSubscription(upstream, ps));
        }
    }

    @SuppressWarnings({ "rawtypes", "SubscriberImplementation" })
    static final class PublishSubscriber<T> implements Cancellable, MultiSubscriber<T>, ContextSupport {

        /**
         * Indicates an empty array of inner subscribers.
         */
        static final InnerSubscriber[] EMPTY = new InnerSubscriber[0];
        /**
         * Indicates a terminated PublishSubscriber.
         */
        static final InnerSubscriber[] TERMINATED = new InnerSubscriber[0];

        /**
         * Holds onto the current connected PublishSubscriber.
         */
        final AtomicReference<PublishSubscriber<T>> current;
        /**
         * The prefetch buffer size.
         */
        final int bufferSize;

        /**
         * Tracks the subscribed InnerSubscribers.
         */
        final AtomicReference<InnerSubscriber<T>[]> subscribers;
        /**
         * Atomically changed from false to true by connect to make sure the
         * connection is only performed by one thread.
         */
        final AtomicBoolean shouldConnect;

        final AtomicReference<Subscription> upstream = new AtomicReference<>();

        /**
         * Contains either the failure or the `COMPLETED` object.
         */
        private final AtomicReference<Throwable> failureOrCompletion = new AtomicReference<>();

        private static final Throwable COMPLETED = new Exception();

        private final Queue<T> queue;

        private final AtomicBoolean cancelled = new AtomicBoolean();

        private final AtomicInteger wip = new AtomicInteger();

        private final Context context;

        @SuppressWarnings("unchecked")
        PublishSubscriber(AtomicReference<PublishSubscriber<T>> current,
                int bufferSize, Context context) {
            this.context = context;
            this.subscribers = new AtomicReference<>(EMPTY);
            this.current = current;
            this.shouldConnect = new AtomicBoolean();
            this.bufferSize = bufferSize;
            this.queue = (Queue<T>) Queues.get(bufferSize).get();
        }

        @Override
        public void cancel() {
            cancelled.set(true);
            if (subscribers.get() != TERMINATED) {
                @SuppressWarnings("unchecked")
                InnerSubscriber[] ps = subscribers.getAndSet(TERMINATED);
                if (ps != TERMINATED) {
                    current.compareAndSet(PublishSubscriber.this, null);
                    Subscriptions.cancel(upstream);
                }
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (this.upstream.compareAndSet(null, s)) {
                s.request(bufferSize);
            }
        }

        @Override
        public void onItem(T t) {
            if (!queue.offer(t)) {
                onFailure(new BackPressureFailure("Queue is full"));
                return;
            }
            drain();
        }

        @Override
        public void onFailure(Throwable e) {
            if (failureOrCompletion.compareAndSet(null, e)) {
                drain();
            }
        }

        @Override
        public void onCompletion() {
            if (failureOrCompletion.compareAndSet(null, COMPLETED)) {
                drain();
            }
        }

        /**
         * Atomically try adding a new InnerSubscriber to this Subscriber or return false if this
         * Subscriber was terminated.
         *
         * @param producer the producer to add
         * @return true if succeeded, false otherwise
         */
        boolean add(InnerSubscriber<T> producer) {
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // get the current producer array
                InnerSubscriber<T>[] c = subscribers.get();
                // if this subscriber-to-source reached a terminal state by receiving
                // an onError or onComplete, just refuse to add the new producer
                if (c == TERMINATED) {
                    return false;
                }
                // we perform a copy-on-write logic
                int len = c.length;
                @SuppressWarnings("unchecked")
                InnerSubscriber<T>[] u = new InnerSubscriber[len + 1];
                System.arraycopy(c, 0, u, 0, len);
                u[len] = producer;
                // try setting the subscribers array
                if (subscribers.compareAndSet(c, u)) {
                    return true;
                }
                // if failed, some other operation succeeded (another add, remove or termination)
                // so retry
            }
        }

        /**
         * Atomically removes the given InnerSubscriber from the subscribers array.
         *
         * @param producer the producer to remove
         */
        @SuppressWarnings("unchecked")
        void remove(InnerSubscriber<T> producer) {
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // let's read the current subscribers array
                InnerSubscriber<T>[] c = subscribers.get();
                int len = c.length;
                // if it is either empty or terminated, there is nothing to remove so we quit
                if (len == 0) {
                    break;
                }
                // let's find the supplied producer in the array
                // although this is O(n), we don't expect too many child subscribers in general
                int j = -1;
                for (int i = 0; i < len; i++) {
                    if (c[i].equals(producer)) {
                        j = i;
                        break;
                    }
                }
                // we didn't find it so just quit
                if (j < 0) {
                    return;
                }
                // we do copy-on-write logic here
                InnerSubscriber<T>[] u;
                // we don't create a new empty array if producer was the single inhabitant
                // but rather reuse an empty array
                if (len == 1) {
                    u = EMPTY;
                } else {
                    // otherwise, create a new array one less in size
                    u = new InnerSubscriber[len - 1];
                    // copy elements being before the given producer
                    System.arraycopy(c, 0, u, 0, j);
                    // copy elements being after the given producer
                    System.arraycopy(c, j + 1, u, j, len - j - 1);
                }
                // try setting this new array as
                if (subscribers.compareAndSet(c, u)) {
                    break;
                }
                // if we failed, it means something else happened
                // (a concurrent add/remove or termination), we need to retry
            }
        }

        /**
         * Perform termination actions in case the source has terminated in some way and
         * the queue has also become empty.
         *
         * @param term the terminal event (a NotificationLite.error or completed)
         * @param empty set to true if the queue is empty
         * @return true if there is indeed a terminal condition
         */
        @SuppressWarnings("unchecked")
        boolean isEmptyOrCompleted(Throwable term, boolean empty) {
            // first of all, check if there is actually a terminal event
            if (term != null) {
                // is it a completion event (impl. note, this is much cheaper than checking for isError)
                if (term == COMPLETED) {
                    // but we also need to have an empty queue
                    if (empty) {
                        // this will prevent OnSubscribe spinning on a terminated but
                        // not yet cancelled PublishSubscriber
                        current.compareAndSet(this, null);
                        /*
                         * This will swap in a terminated array so add() in OnSubscribe will reject
                         * child subscribers to associate themselves with a terminated and thus
                         * never again emitting chain.
                         *
                         * Since we atomically change the contents of 'subscribers' only one
                         * operation wins at a time. If an add() wins before this getAndSet,
                         * its value will be part of the returned array by getAndSet and thus
                         * will receive the terminal notification. Otherwise, if getAndSet wins,
                         * add() will refuse to add the child producer and will trigger the
                         * creation of subscriber-to-source.
                         */
                        for (InnerSubscriber<?> actual : subscribers.getAndSet(TERMINATED)) {
                            actual.downstream.onComplete();
                        }
                        // indicate we reached the terminal state
                        return true;
                    }
                } else {
                    // term is the failure.

                    // this will prevent OnSubscribe spinning on a terminated
                    // but not yet cancelled PublishSubscriber
                    current.compareAndSet(this, null);
                    // this will swap in a terminated array so add() in OnSubscribe will reject
                    // child subscribers to associate themselves with a terminated and thus
                    // never again emitting chain
                    for (InnerSubscriber<?> actual : subscribers.getAndSet(TERMINATED)) {
                        actual.downstream.onError(term);
                    }
                    // indicate we reached the terminal state
                    return true;
                }
            }
            // there is still work to be done
            return false;
        }

        /**
         * The common serialization point of events arriving from upstream and downstream subscribers
         * requesting more.
         */
        void drain() {
            // standard construct of queue-drain
            // if there is an emission going on, indicate that more work needs to be done
            // the exact nature of this work needs to be determined from other data structures
            if (wip.getAndIncrement() != 0) {
                return;
            }
            int missed = 1;

            // saving a local copy because this will be accessed after every item
            // delivered to detect changes in the subscribers due to an onNext
            // and thus not dropping items
            AtomicReference<InnerSubscriber<T>[]> subscribers = this.subscribers;

            // We take a snapshot of the current child subscribers.
            // Concurrent subscribers may miss this iteration, but it is to be expected
            InnerSubscriber<T>[] ps = subscribers.get();

            outer: for (;;) {
                /*
                 * We need to read terminalEvent before checking the queue for emptiness because
                 * all enqueue happens before setting the terminal event.
                 * If it were the other way around, when the emission is paused between
                 * checking isEmpty and checking terminalEvent, some other thread might
                 * have produced elements and set the terminalEvent and we'd quit emitting
                 * prematurely.
                 */
                Throwable term = failureOrCompletion.get();
                /*
                 * See if the queue is empty; since we need this information multiple
                 * times later on, we read it once.
                 * Although the queue can become non-empty in the mean time, we will
                 * detect it through the missing flag and will do another iteration.
                 */
                Queue<T> q = queue;
                boolean isEmpty = q.isEmpty();
                // if the queue is empty and the terminal event was received, quit.
                if (isEmptyOrCompleted(term, isEmpty)) {
                    return;
                }

                // We have elements queued. Note that due to the serialization nature of dispatch()
                // this loop is the only one which can turn a non-empty queue into an empty one
                // and as such, no need to ask the queue itself again for that.
                if (!isEmpty) {

                    int len = ps.length;
                    // Let's assume everyone requested the maximum value.
                    long maxRequested = Long.MAX_VALUE;
                    // count how many have triggered cancellation
                    int cancelled = 0;

                    // Now find the minimum amount each child-subscriber requested
                    // since we can only emit that much to all of them without violating
                    // backpressure constraints
                    for (InnerSubscriber<T> ip : ps) {
                        long r = ip.requested.get();
                        // if there is one child subscriber that hasn't requested yet
                        // we can't emit anything to anyone
                        if (r != Long.MIN_VALUE) {
                            maxRequested = Math.min(maxRequested, r - ip.emitted);
                        } else {
                            cancelled++;
                        }
                    }

                    // it may happen everyone has cancelled between here and subscribers.get()
                    // or we have no subscribers at all to begin with
                    if (len == cancelled) {
                        term = failureOrCompletion.get();
                        // so let's consume a value from the queue
                        T v;

                        try {
                            v = q.poll();
                        } catch (Throwable ex) {
                            upstream.get().cancel();
                            term = ex;
                            failureOrCompletion.set(term);
                            v = null;
                        }
                        // or terminate if there was a terminal event and the queue is empty
                        if (isEmptyOrCompleted(term, v == null)) {
                            return;
                        }
                        // otherwise, just ask for a new value
                        upstream.get().request(1);
                        // and retry emitting to potential new child subscribers
                        continue;
                    }
                    // if we get here, it means there are non-cancelled child subscribers
                    // and we count the number of emitted values because the queue
                    // may contain less than requested
                    int d = 0;
                    while (d < maxRequested) {
                        term = failureOrCompletion.get();
                        T v;

                        try {
                            v = q.poll();
                        } catch (Throwable ex) {
                            upstream.get().cancel();
                            term = ex;
                            failureOrCompletion.set(term);
                            v = null;
                        }

                        isEmpty = v == null;
                        // let's check if there is a terminal event and the queue became empty just now
                        if (isEmptyOrCompleted(term, isEmpty)) {
                            return;
                        }
                        // the queue is empty but we aren't terminated yet, finish this emission loop
                        if (isEmpty) {
                            break;
                        }
                        T value = v;
                        boolean subscribersChanged = false;

                        // let's emit this value to all child subscribers
                        for (InnerSubscriber<T> ip : ps) {
                            // if ip.get() is negative, the child has either cancelled in the
                            // meantime or hasn't requested anything yet
                            // this eager behavior will skip cancelled children in case
                            // multiple values are available in the queue
                            long ipr = ip.requested.get();
                            if (ipr != Long.MIN_VALUE) {
                                if (ipr != Long.MAX_VALUE) {
                                    // indicate this child has received 1 element
                                    ip.emitted++;
                                }
                                ip.downstream.onNext(value);
                            } else {
                                subscribersChanged = true;
                            }
                        }
                        // indicate we emitted one element
                        d++;

                        // see if the array of subscribers changed as a consequence
                        // of emission or concurrent activity
                        InnerSubscriber<T>[] freshArray = subscribers.get();
                        if (subscribersChanged || freshArray != ps) {
                            ps = freshArray;

                            // if we did emit at least one element, request more to replenish the queue
                            if (d != 0) {
                                upstream.get().request(d);
                            }

                            continue outer;
                        }
                    }

                    // if we did emit at least one element, request more to replenish the queue
                    if (d != 0) {
                        upstream.get().request(d);
                    }
                    // if we have requests but not an empty queue after emission
                    // let's try again to see if more requests/child subscribers are
                    // ready to receive more
                    if (maxRequested != 0L && !isEmpty) {
                        continue;
                    }
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }

                // get a fresh copy of the current subscribers
                ps = subscribers.get();
            }
        }

        @Override
        public Context context() {
            return this.context;
        }
    }

    /**
     * A Subscription that manages the request and cancellation state of a
     * child subscriber in thread-safe manner.
     *
     * @param <T> the value type
     */
    static final class InnerSubscriber<T> implements Subscription {

        /**
         * Requested number of items.
         */
        private final AtomicLong requested = new AtomicLong();

        /**
         * The actual child subscriber.
         */
        private final Subscriber<? super T> downstream;
        /**
         * The parent subscriber-to-source used to allow removing the child in case of
         * child cancellation.
         */
        private final AtomicReference<PublishSubscriber<T>> parent = new AtomicReference<>();

        /**
         * Track the number of emitted items (avoids decrementing the request counter).
         */
        long emitted;

        InnerSubscriber(Subscriber<? super T> child) {
            this.downstream = child;
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                long r = requested.get();
                if (r != Long.MIN_VALUE && r != Long.MAX_VALUE) {
                    Subscriptions.add(requested, n);
                }
                PublishSubscriber<T> p = parent.get();
                if (p != null) {
                    p.drain();
                }
            }
        }

        @Override
        public void cancel() {
            long requests = requested.get();
            // let's see if we are cancelled
            if (requests != CANCELLED) {
                requests = requested.getAndSet(CANCELLED);
                if (requests != CANCELLED) {
                    PublishSubscriber<T> p = parent.get();
                    if (p != null) {
                        p.remove(this);
                        // After removal, we might have unblocked the other child subscribers:
                        // let's assume this child had 0 requested before the cancellation while
                        // the others had non-zero. By removing this 'blocking' child, the others
                        // are now free to receive events
                        p.drain();
                    }
                }
            }
        }
    }

    @SuppressWarnings("PublisherImplementation")
    static final class InnerPublisher<T> implements Publisher<T> {
        private final AtomicReference<PublishSubscriber<T>> curr;
        private final int bufferSize;

        InnerPublisher(AtomicReference<PublishSubscriber<T>> curr, int bufferSize) {
            this.curr = curr;
            this.bufferSize = bufferSize;
        }

        @Override
        public void subscribe(Subscriber<? super T> child) {
            Context context;
            if (child instanceof ContextSupport) {
                context = ((ContextSupport) child).context();
            } else {
                context = Context.empty();
            }
            InnerSubscriber<T> inner = new InnerSubscriber<>(child);
            child.onSubscribe(inner);
            // concurrent connection/disconnection may change the state,
            // we loop to be atomic while the child subscribes
            for (;;) {
                // get the current subscriber-to-source
                PublishSubscriber<T> r = curr.get();
                // if there isn't one or it is cancelled/disposed
                if (r == null || r.cancelled.get()) {
                    // create a new subscriber to source
                    PublishSubscriber<T> u = new PublishSubscriber<>(curr, bufferSize, context);
                    // let's try setting it as the current subscriber-to-source
                    if (!curr.compareAndSet(r, u)) {
                        // didn't work, maybe someone else did it or the current subscriber
                        // to source has just finished
                        continue;
                    }
                    // we won, let's use it going onwards
                    r = u;
                }

                /*
                 * Try adding it to the current subscriber-to-source, add is atomic in respect
                 * to other adds and the termination of the subscriber-to-source.
                 */
                if (r.add(inner)) {
                    if (inner.requested.get() == CANCELLED) {
                        r.remove(inner);
                    } else {
                        inner.parent.set(r);
                    }
                    r.drain();
                    break;
                }
            }
        }
    }
}
