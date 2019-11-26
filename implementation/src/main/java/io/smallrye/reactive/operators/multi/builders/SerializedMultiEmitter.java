package io.smallrye.reactive.operators.multi.builders;

import static io.smallrye.reactive.helpers.Subscriptions.TERMINATED;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.helpers.queues.SpscLinkedArrayQueue;
import io.smallrye.reactive.subscription.MultiEmitter;

/**
 * Serializes calls to onNext, onError and onComplete.
 *
 * @param <T> the type of item
 */
@SuppressWarnings("SubscriberImplementation")
public class SerializedMultiEmitter<T> implements MultiEmitter<T>, Subscriber<T> {

    private final AtomicInteger wip = new AtomicInteger();
    private final BaseMultiEmitter<T> downstream;
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final SpscLinkedArrayQueue<T> queue = new SpscLinkedArrayQueue<>(16);

    private volatile boolean done;

    SerializedMultiEmitter(BaseMultiEmitter<T> downstream) {
        this.downstream = downstream;
    }

    @Override
    public void onSubscribe(Subscription s) {

    }

    @Override
    public void onNext(T item) {
        if (downstream.isCancelled() || done) {
            return;
        }
        if (item == null) {
            onError(new NullPointerException(
                    "onNext called with null. Null values are generally not allowed in 2.x operators and sources."));
            return;
        }
        if (wip.compareAndSet(0, 1)) {
            downstream.emit(item);
            if (wip.decrementAndGet() == 0) {
                return;
            }
        } else {
            SpscLinkedArrayQueue<T> q = queue;
            synchronized (q) {
                q.offer(item);
            }
            if (wip.getAndIncrement() != 0) {
                return;
            }
        }
        drainLoop();
    }

    @Override
    public void onError(Throwable failure) {
        if (downstream.isCancelled() || done) {
            return;
        }
        if (failure == null) {
            failure = new NullPointerException("failure cannot be `null`");
        }
        if (this.failure.compareAndSet(null, failure)) {
            done = true;
            drain();
        }
    }

    @Override
    public void onComplete() {
        if (downstream.isCancelled() || done) {
            return;
        }
        done = true;
        drain();
    }

    void drain() {
        if (wip.getAndIncrement() == 0) {
            drainLoop();
        }
    }

    void drainLoop() {
        BaseMultiEmitter<T> emitter = this.downstream;
        Queue<T> q = queue;
        int missed = 1;
        for (;;) {

            for (;;) {
                if (emitter.isCancelled()) {
                    q.clear();
                    return;
                }

                if (this.failure.get() != null) {
                    q.clear();
                    emitter.fail(this.failure.getAndSet(TERMINATED));
                    return;
                }
                boolean isDone = done;
                T item = q.poll();
                boolean isEmpty = item == null;
                if (isDone && isEmpty) {
                    emitter.complete();
                    return;
                }

                if (isEmpty) {
                    break;
                }

                emitter.emit(item);
            }

            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    @Override
    public MultiEmitter<T> emit(T item) {
        onNext(item);
        return this;
    }

    @Override
    public void fail(Throwable failure) {
        onError(failure);
    }

    @Override
    public void complete() {
        onComplete();
    }

    @Override
    public MultiEmitter<T> onTermination(Runnable onTermination) {
        downstream.onTermination(onTermination);
        return this;
    }
}
