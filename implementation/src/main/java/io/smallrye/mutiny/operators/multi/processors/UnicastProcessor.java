package io.smallrye.mutiny.operators.multi.processors;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Implementation of a processor using a queue to store items and allows a single subscriber to receive
 * these items.
 * <p>
 * The back pressure model is not using the request protocol but the queue used to store the items. If the queue
 * gets full, an {@link io.smallrye.mutiny.subscription.BackPressureFailure} exception is propagated downstream.
 *
 * @param <T> the type of item
 */
public class UnicastProcessor<T> extends AbstractMulti<T> implements Processor<T, T>, Subscription {

    private final Runnable onTermination;
    private final Queue<T> queue;

    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicInteger wip = new AtomicInteger();
    private final AtomicLong requested = new AtomicLong();
    private final AtomicReference<Subscriber<? super T>> downstream = new AtomicReference<>();
    private volatile boolean hasUpstream;

    /**
     * Creates a new {@link UnicastProcessor} using a new unbounded queue.
     *
     * @param <I> the type of item
     * @return the unicast processor
     */
    public static <I> UnicastProcessor<I> create() {
        return new UnicastProcessor<>(Queues.<I> unbounded(Queues.BUFFER_S).get(), null);
    }

    /**
     * Creates a new {@link UnicastProcessor} using the given queue.
     *
     * @param queue the queue, must not be {@code null}
     * @param onTermination the termination callback, can be {@code null}
     * @param <I> the type of item
     * @return the unicast processor
     */
    public static <I> UnicastProcessor<I> create(Queue<I> queue, Runnable onTermination) {
        return new UnicastProcessor<>(queue, onTermination);
    }

    private UnicastProcessor(Queue<T> queue, Runnable onTermination) {
        this.queue = ParameterValidation.nonNull(queue, "queue");
        this.onTermination = onTermination;
    }

    private void onTerminate() {
        if (onTermination != null) {
            onTermination.run();
        }
    }

    void drainWithDownstream(Subscriber<? super T> actual) {
        int missed = 1;

        final Queue<T> q = queue;

        for (;;) {

            long r = requested.get();
            long e = 0L;

            while (r != e) {
                boolean d = done.get();

                T t = q.poll();
                boolean empty = t == null;

                if (isCancelledOrDone(d, empty)) {
                    return;
                }

                if (empty) {
                    break;
                }

                actual.onNext(t);

                e++;
            }

            if (r == e) {
                if (isCancelledOrDone(done.get(), q.isEmpty())) {
                    return;
                }
            }

            if (e != 0 && r != Long.MAX_VALUE) {
                requested.addAndGet(-e);
            }

            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    private void drain() {
        if (wip.getAndIncrement() != 0) {
            return;
        }

        int missed = 1;
        for (;;) {
            Subscriber<? super T> actual = downstream.get();
            if (actual != null) {
                drainWithDownstream(actual);
                return;
            }
            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    private boolean isCancelledOrDone(boolean isDone, boolean isEmpty) {
        Subscriber<? super T> subscriber = downstream.get();
        if (cancelled.get()) {
            queue.clear();
            return true;
        }
        if (isDone && isEmpty) {
            Throwable failed = failure.get();
            if (failed != null) {
                subscriber.onError(failed);
            } else {
                subscriber.onComplete();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onSubscribe(Subscription upstream) {
        if (hasUpstream) {
            upstream.cancel();
            return;
        }
        if (isDoneOrCancelled()) {
            upstream.cancel();
        } else {
            hasUpstream = true;
            // Request max, it's the queue that buffer the items.
            upstream.request(Long.MAX_VALUE);
        }
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        ParameterValidation.nonNull(downstream, "downstream");
        if (this.downstream.compareAndSet(null, downstream)) {
            downstream.onSubscribe(this);
            if (!cancelled.get()) {
                drain();
            }
        } else {
            Subscriptions.fail(downstream, new IllegalStateException("Already subscribed"));
        }
    }

    @Override
    public synchronized void onNext(T t) {
        if (isDoneOrCancelled()) {
            return;
        }
        if (!queue.offer(t)) {
            Throwable overflow = new BackPressureFailure("the queue is full");
            onError(overflow);
            return;
        }
        drain();
    }

    private boolean isDoneOrCancelled() {
        return done.get() || cancelled.get();
    }

    @Override
    public void onError(Throwable failure) {
        Objects.requireNonNull(failure);
        if (isDoneOrCancelled()) {
            return;
        }

        onTerminate();
        this.failure.set(failure);
        this.done.set(true);

        drain();
    }

    @Override
    public void onComplete() {
        if (isDoneOrCancelled()) {
            return;
        }
        onTerminate();
        this.done.set(true);
        drain();
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
            onTerminate();
            if (wip.getAndIncrement() == 0) {
                queue.clear();
            }
            downstream.set(null);
        }
    }

    /**
     * Checks whether there is a subscriber listening for the emitted events.
     * Mostly for testing purpose.
     *
     * @return {@code true} if there is a subscriber, {@code false} otherwise
     */
    public boolean hasSubscriber() {
        return downstream.get() != null;
    }

    public SerializedProcessor<T, T> serialized() {
        return new SerializedProcessor<>(this);
    }
}
