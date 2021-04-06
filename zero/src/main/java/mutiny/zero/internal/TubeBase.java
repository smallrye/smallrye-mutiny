package mutiny.zero.internal;

import static java.util.Objects.requireNonNull;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import mutiny.zero.Tube;

public abstract class TubeBase<T> implements Tube<T>, Subscription {

    protected final Subscriber<? super T> subscriber;

    protected volatile boolean cancelled;
    protected final AtomicInteger wip = new AtomicInteger();
    protected final AtomicLong requested = new AtomicLong();
    protected final ConcurrentLinkedQueue<T> dispatchQueue = new ConcurrentLinkedQueue<>();

    protected volatile Throwable failure;
    protected volatile boolean completed = false;

    protected Runnable terminationAction = () -> {
        // Do nothing
    };

    protected LongConsumer requestConsumer = count -> {
        // Do nothing
    };

    protected Runnable cancellationAction = () -> {
        // Do nothing
    };

    protected TubeBase(Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
    }

    // ---- Subscription API ---- //

    @Override
    public void request(long n) {
        if (cancelled) {
            return;
        }
        if (n <= 0L) {
            fail(Helper.negativeRequest(n));
        } else {
            Helper.add(requested, n);
            requestConsumer.accept(n);
        }
        drainLoop();
    }

    @Override
    public void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        dispatchQueue.clear();
        cancellationAction.run();
        terminationAction.run();
    }

    // ---- Tube API ---- //

    @Override
    public Tube<T> send(T item) {
        if (cancelled()) {
            return this;
        }
        if (item == null) {
            fail(new NullPointerException("The item is null"));
        } else {
            handleItem(item);
        }
        return this;
    }

    @Override
    public void fail(Throwable err) {
        if (cancelled) {
            return;
        }
        if (err == null) {
            err = new NullPointerException("The error is null");
        }
        failure = err;
        drainLoop();
    }

    @Override
    public void complete() {
        if (completed) {
            throw new IllegalStateException("Already completed");
        }
        if (cancelled) {
            return;
        }
        completed = true;
        drainLoop();
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public long outstandingRequests() {
        return requested.get();
    }

    @Override
    public Tube<T> whenCancelled(Runnable action) {
        requireNonNull(action, "The cancellation action cannot be null");
        this.cancellationAction = action;
        return this;
    }

    @Override
    public Tube<T> whenTerminates(Runnable action) {
        requireNonNull(action, "The termination action cannot be null");
        this.terminationAction = action;
        return this;
    }

    @Override
    public Tube<T> whenRequested(LongConsumer consumer) {
        requireNonNull(consumer, "The request consumer cannot be null");
        this.requestConsumer = consumer;
        long outstanding = outstandingRequests();
        if (outstanding > 0L) {
            consumer.accept(outstanding);
        }
        return this;
    }

    // ---- Drain loop ---- //

    protected abstract void handleItem(T item);

    protected void drainLoop() {
        if (wip.getAndIncrement() != 0) {
            // Another tread is working
            return;
        }

        int missed = 1;
        Queue<T> queue = dispatchQueue;
        while (missed != 0) {
            long emitted = 0L;
            long pending = outstandingRequests();

            if (cancelled) {
                queue.clear();
                cancellationAction.run();
                return;
            }

            do {
                if (cancelled) {
                    queue.clear();
                    cancellationAction.run();
                    return;
                }

                T item = queue.poll();
                if (item == null && completed) {
                    cancelled = true;
                    if (failure != null) {
                        subscriber.onError(failure);
                    } else {
                        subscriber.onComplete();
                    }
                    terminationAction.run();
                    return;
                }
                if (item == null) {
                    break;
                }

                subscriber.onNext(item);
                emitted++;
            } while (emitted != pending);

            if (emitted > 0) {
                requested.addAndGet(-emitted);
            }

            if (cancelled) {
                queue.clear();
                return;
            }
            if (failure != null) {
                cancelled = true;
                subscriber.onError(failure);
                terminationAction.run();
                return;
            } else if (queue.isEmpty() && completed) {
                cancelled = true;
                subscriber.onComplete();
                terminationAction.run();
                return;
            }

            missed = wip.addAndGet(-missed);
        }
    }
}
