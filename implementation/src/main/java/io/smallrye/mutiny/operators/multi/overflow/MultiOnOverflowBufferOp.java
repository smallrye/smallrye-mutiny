
package io.smallrye.mutiny.operators.multi.overflow;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Queue;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnOverflowBufferOp<T> extends AbstractMultiOperator<T, T> {

    private final int bufferSize;
    private final boolean unbounded;
    private final Consumer<T> dropConsumer;
    private final Function<T, Uni<?>> dropUniMapper;

    public MultiOnOverflowBufferOp(Multi<T> upstream, int bufferSize, boolean unbounded, Consumer<T> dropConsumer,
            Function<T, Uni<?>> dropUniMapper) {
        super(upstream);
        this.bufferSize = bufferSize;
        this.unbounded = unbounded;
        this.dropConsumer = dropConsumer;
        this.dropUniMapper = dropUniMapper;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        OnOverflowBufferProcessor subscriber = new OnOverflowBufferProcessor(downstream, bufferSize, unbounded);
        upstream.subscribe().withSubscriber(subscriber);
    }

    class OnOverflowBufferProcessor extends MultiOperatorProcessor<T, T> {

        private final Queue<T> queue;

        Throwable failure;

        private final AtomicLong requested = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();

        volatile boolean cancelled;
        volatile boolean done;

        OnOverflowBufferProcessor(MultiSubscriber<? super T> downstream, int bufferSize, boolean unbounded) {
            super(downstream);
            this.queue = unbounded ? Queues.<T> unbounded(bufferSize).get() : Queues.createStrictSizeQueue(bufferSize);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (compareAndSetUpstreamSubscription(null, subscription)) {
                downstream.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onItem(T t) {
            if (!queue.offer(t)) {
                BackPressureFailure bpf = new BackPressureFailure(
                        "The overflow buffer is full, which is due to the upstream sending too many items w.r.t. the downstream capacity and/or the downstream not consuming items fast enough");
                if (dropUniMapper != null) {
                    notifyOnOverflowCall(t, bpf);
                } else {
                    notifyOnOverflowInvoke(t, bpf);
                }
            } else {
                drain();
            }
        }

        private void notifyOnOverflowInvoke(T t, BackPressureFailure bpf) {
            if (dropConsumer != null) {
                try {
                    dropConsumer.accept(t);
                } catch (Throwable e) {
                    bpf.addSuppressed(e);
                }
            }
            onFailure(bpf);
        }

        private void notifyOnOverflowCall(T t, BackPressureFailure bpf) {
            MultiSubscriber<? super T> subscriber = this.downstream;
            super.cancel();
            try {
                Uni<?> uni = nonNull(dropUniMapper.apply(t), "uni");
                uni.subscribe().with(
                        ignored -> subscriber.onFailure(bpf),
                        failure -> {
                            bpf.addSuppressed(failure);
                            subscriber.onFailure(bpf);
                        });
            } catch (Throwable failure) {
                bpf.addSuppressed(failure);
                subscriber.onFailure(bpf);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            this.failure = failure;
            done = true;
            drain();
        }

        @Override
        public void onCompletion() {
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
                for (;;) {

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
                        downstream.onItem(item);
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
                if (failure != null) {
                    queue.clear();
                    if (failure instanceof BackPressureFailure) {
                        MultiSubscriber<? super T> subscriber = this.downstream;
                        super.cancel();
                        subscriber.onFailure(failure);
                    } else {
                        super.onFailure(failure);
                    }
                    return true;
                } else if (wasEmpty) {
                    super.onCompletion();
                    return true;
                }
            }
            return false;
        }
    }

}
