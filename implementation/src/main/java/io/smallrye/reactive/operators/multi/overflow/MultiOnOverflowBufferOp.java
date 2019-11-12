

package io.smallrye.reactive.operators.multi.overflow;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.helpers.queues.SpscArrayQueue;
import io.smallrye.reactive.helpers.queues.SpscLinkedArrayQueue;
import io.smallrye.reactive.operators.multi.AbstractMultiWithUpstream;
import io.smallrye.reactive.operators.multi.MultiOperatorSubscriber;
import io.smallrye.reactive.subscription.BackPressureFailure;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class MultiOnOverflowBufferOp<T> extends AbstractMultiWithUpstream<T, T> {

    private final int bufferSize;
    private final boolean unbounded;
    private final boolean postponeFailurePropagation;
    private final Consumer<T> onOverflow;

    public MultiOnOverflowBufferOp(Multi<T> upstream, int bufferSize, boolean unbounded,
            boolean postponeFailurePropagation, Consumer<T> onOverflow) {
        super(upstream);
        this.bufferSize = bufferSize;
        this.unbounded = unbounded;
        this.postponeFailurePropagation = postponeFailurePropagation;
        this.onOverflow = onOverflow;
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        OnOverflowBufferSubscriber<T> subscriber = new OnOverflowBufferSubscriber<>(downstream,
                bufferSize, unbounded,
                postponeFailurePropagation,
                onOverflow);
        upstream.subscribe(subscriber);
    }

    static final class OnOverflowBufferSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        private final Queue<T> queue;
        private final boolean postponeFailurePropagation;
        private final Consumer<T> onOverflow;

        Throwable failure;

        private final AtomicLong requested = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();

        volatile boolean cancelled;
        volatile boolean done;

        OnOverflowBufferSubscriber(Subscriber<? super T> downstream, int bufferSize,
                boolean unbounded, boolean postponeFailurePropagation, Consumer<T> onOverflow) {
            super(downstream);
            this.onOverflow = onOverflow;
            this.postponeFailurePropagation = postponeFailurePropagation;
            this.queue = unbounded ? new SpscLinkedArrayQueue<>(bufferSize) : new SpscArrayQueue<>(bufferSize);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                downstream.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onNext(T t) {
            if (!queue.offer(t)) {
                BackPressureFailure ex = new BackPressureFailure(
                        "Buffer is full due to lack of downstream consumption");
                try {
                    onOverflow.accept(t);
                } catch (Throwable e) {
                    ex.initCause(e);
                }
                onError(ex);
                return;
            }
            drain();
        }

        @Override
        public void onError(Throwable failure) {
            this.failure = failure;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            done = true;
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
            if (!cancelled) {
                cancelled = true;
                super.cancel();

                if (wip.getAndIncrement() == 0) {
                    queue.clear();
                }
            }
        }

        void drain() {
            if (wip.getAndIncrement() == 0) {
                int missed = 1;
                final Queue<T> qe = queue;
                for (; ; ) {

                    if (checkTerminated(done, qe.isEmpty())) {
                        return;
                    }

                    long emitted = 0L;
                    long req = requested.get();

                    while (emitted != req) {
                        boolean wasDone = done;
                        T item = qe.poll();
                        boolean wasEmpty = item == null;
                        if (checkTerminated(wasDone, wasEmpty)) {
                            return;
                        }
                        if (wasEmpty) {
                            break;
                        }
                        downstream.onNext(item);
                        emitted++;
                    }

                    if (emitted == req) {
                        boolean d = done;
                        boolean empty = qe.isEmpty();
                        if (checkTerminated(d, empty)) {
                            return;
                        }
                    }

                    if (emitted != 0L) {
                        if (req != Long.MAX_VALUE) {
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

        boolean checkTerminated(boolean wasDone, boolean wasEmpty) {
            if (cancelled) {
                queue.clear();
                return true;
            }
            if (wasDone) {
                if (postponeFailurePropagation) {
                    if (wasEmpty) {
                        if (failure != null) {
                            super.onError(failure);
                        } else {
                            super.onComplete();
                        }
                        return true;
                    }
                } else {
                    if (failure != null) {
                        queue.clear();
                        super.onError(failure);
                        return true;
                    } else if (wasEmpty) {
                        super.onComplete();
                        return true;
                    }
                }
            }
            return false;
        }
    }

}
