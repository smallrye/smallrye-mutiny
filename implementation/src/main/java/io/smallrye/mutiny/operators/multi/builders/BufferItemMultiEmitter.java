package io.smallrye.mutiny.operators.multi.builders;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class BufferItemMultiEmitter<T> extends BaseMultiEmitter<T> {

    private final Queue<T> queue;
    private Throwable failure;
    private volatile boolean done;
    private final AtomicInteger wip = new AtomicInteger();

    BufferItemMultiEmitter(MultiSubscriber<? super T> actual, int capacityHint) {
        super(actual);
        this.queue = Queues.<T> unbounded(capacityHint).get();
    }

    @Override
    public MultiEmitter<T> emit(T t) {
        if (done || isCancelled()) {
            return this;
        }

        if (t == null) {
            fail(new NullPointerException("`emit` called with `null`."));
            return this;
        }
        queue.offer(t);
        drain();
        return this;
    }

    @Override
    public void failed(Throwable failure) {
        if (done || isCancelled()) {
            return;
        }

        if (failure == null) {
            failure = new NullPointerException("onError called with null.");
        }

        this.failure = failure;
        done = true;
        drain();
    }

    @Override
    public void completion() {
        done = true;
        drain();
    }

    @Override
    void onRequested() {
        drain();
    }

    @Override
    void onUnsubscribed() {
        if (wip.getAndIncrement() == 0) {
            queue.clear();
        }
    }

    void drain() {
        if (wip.getAndIncrement() != 0) {
            return;
        }

        int missed = 1;
        final Queue<T> q = queue;

        do {
            long r = requested.get();
            long e = 0L;

            while (e != r) {
                if (isCancelled()) {
                    q.clear();
                    return;
                }

                boolean d = done;

                T o = q.poll();

                boolean empty = o == null;

                if (d && empty) {
                    if (failure != null) {
                        super.failed(failure);
                    } else {
                        super.completion();
                    }
                    return;
                }

                if (empty) {
                    break;
                }

                try {
                    downstream.onItem(o);
                } catch (Throwable x) {
                    cancel();
                }

                e++;
            }

            if (e == r) {
                if (isCancelled()) {
                    q.clear();
                    return;
                }

                boolean d = done;

                boolean empty = q.isEmpty();

                if (d && empty) {
                    if (failure != null) {
                        super.failed(failure);
                    } else {
                        super.completion();
                    }
                    return;
                }
            }

            if (e != 0) {
                Subscriptions.produced(requested, e);
            }

            missed = wip.addAndGet(-missed);
        } while (missed != 0);
    }
}
