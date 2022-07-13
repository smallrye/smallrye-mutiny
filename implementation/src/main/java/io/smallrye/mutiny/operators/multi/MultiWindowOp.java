package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Splits the source sequence by time of reception into potentially overlapping {@code Multi}.
 *
 * @param <T> the type of item from upstream
 */
public class MultiWindowOp<T> extends AbstractMultiOperator<T, Multi<T>> {

    /**
     * Number of items in the window
     */
    private final int size;
    /**
     * Number of items to skip before starting a new window.
     */
    private final int skip;

    /**
     * Queue supplier
     */
    private final Supplier<? extends Queue<T>> processorQueueSupplier;

    /**
     * Overflow queue supplier.
     */
    private final Supplier<? extends Queue<UnicastProcessor<T>>> overflowQueueSupplier;

    public MultiWindowOp(Multi<? extends T> upstream,
            int size,
            int skip) {
        super(upstream);
        this.size = ParameterValidation.positive(size, "size");
        this.skip = ParameterValidation.positive(skip, "skip");
        this.processorQueueSupplier = Queues.unbounded(Queues.BUFFER_XS);
        this.overflowQueueSupplier = Queues.unbounded(Queues.BUFFER_XS);
    }

    @Override
    public void subscribe(MultiSubscriber<? super Multi<T>> downstream) {
        if (skip == size) {
            upstream.subscribe().withSubscriber(new MultiWindowExactProcessor<>(downstream,
                    size,
                    processorQueueSupplier));
        } else if (skip > size) {
            upstream.subscribe().withSubscriber(new MultiWindowWithSkipProcessor<>(downstream,
                    size, skip, processorQueueSupplier));
        } else {
            upstream.subscribe().withSubscriber(new MultiWindowWithOverlapProcessor<>(downstream,
                    size,
                    skip, processorQueueSupplier, overflowQueueSupplier.get()));
        }
    }

    static final class MultiWindowExactProcessor<T> extends MultiOperatorProcessor<T, Multi<T>> {

        private final Supplier<? extends Queue<T>> supplier;
        private final int size;
        private final AtomicInteger count = new AtomicInteger();

        int index;
        private UnicastProcessor<T> processor;

        MultiWindowExactProcessor(MultiSubscriber<? super Multi<T>> downstream,
                int size,
                Supplier<? extends Queue<T>> supplier) {
            super(downstream);
            this.size = size;
            this.supplier = supplier;
            count.lazySet(1);
        }

        @Override
        public void onItem(T t) {
            if (isDone()) {
                return;
            }

            int i = index;
            UnicastProcessor<T> proc = processor;
            if (!isCancelled() && i == 0) {
                count.getAndIncrement();
                proc = UnicastProcessor.create(supplier.get(), () -> {
                    if (count.decrementAndGet() == 0) {
                        getUpstreamSubscription().cancel();
                    }
                });

                processor = proc;
                downstream.onItem(proc);
            }

            i++;

            proc.onNext(t);

            if (i == size) {
                index = 0;
                processor = null;
                proc.onComplete();
            } else {
                index = i;
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                UnicastProcessor<T> proc = processor;
                if (proc != null) {
                    processor = null;
                    proc.onError(failure);
                }
                downstream.onFailure(failure);
            } else {
                Infrastructure.handleDroppedException(failure);
            }
        }

        @Override
        public void onCompletion() {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                UnicastProcessor<T> proc = processor;
                if (proc != null) {
                    processor = null;
                    proc.onComplete();
                }

                downstream.onCompletion();
            }
        }

        @Override
        public void request(long n) {
            long u = Subscriptions.multiply(size, n);
            super.request(u);
        }

        @Override
        public void cancel() {
            if (compareAndSwapDownstreamCancellationRequest()) {
                if (count.decrementAndGet() == 0) {
                    getUpstreamSubscription().cancel();
                }
            }
        }
    }

    static final class MultiWindowWithSkipProcessor<T> extends MultiOperatorProcessor<T, Multi<T>> {

        private final Supplier<? extends Queue<T>> supplier;
        private final int size;
        private final int skip;

        private final AtomicInteger count = new AtomicInteger();
        private final AtomicBoolean firstRequest = new AtomicBoolean();

        int index;

        UnicastProcessor<T> processor;

        MultiWindowWithSkipProcessor(MultiSubscriber<? super Multi<T>> downstream, int size, int skip,
                Supplier<? extends Queue<T>> supplier) {
            super(downstream);
            this.size = size;
            this.skip = skip;
            this.supplier = supplier;
            count.lazySet(1);
        }

        @Override
        public void onItem(T t) {
            if (isDone()) {
                return;
            }

            int i = index;

            UnicastProcessor<T> proc = processor;
            if (i == 0) {
                count.getAndIncrement();
                proc = UnicastProcessor.create(supplier.get(), () -> {
                    if (count.decrementAndGet() == 0) {
                        getUpstreamSubscription().cancel();
                    }
                });
                processor = proc;
                downstream.onItem(proc);
            }

            i++;

            if (proc != null) {
                proc.onNext(t);
            }

            if (i == size) {
                processor = null;
                if (proc != null) {
                    proc.onComplete();
                }
            }

            if (i == skip) {
                index = 0;
            } else {
                index = i;
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                Processor<T, T> proc = processor;
                if (proc != null) {
                    processor = null;
                    proc.onError(failure);
                }
                downstream.onFailure(failure);
            } else {
                Infrastructure.handleDroppedException(failure);
            }
        }

        @Override
        public void onCompletion() {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                Processor<T, T> proc = processor;
                if (proc != null) {
                    processor = null;
                    proc.onComplete();
                }

                downstream.onCompletion();
            }
        }

        @Override
        public void request(long n) {
            if (firstRequest.compareAndSet(false, true)) {
                long u = Subscriptions.multiply(size, n);
                long v = Subscriptions.multiply(skip - (long) size, n - 1);
                long w = Subscriptions.add(u, v);
                super.request(w);
            } else {
                long u = Subscriptions.multiply(skip, n);
                super.request(u);
            }
        }

        @Override
        public void cancel() {
            if (compareAndSwapDownstreamCancellationRequest()) {
                if (count.decrementAndGet() == 0) {
                    getUpstreamSubscription().cancel();
                }
            }
        }
    }

    static final class MultiWindowWithOverlapProcessor<T> extends MultiOperatorProcessor<T, Multi<T>>
            implements Runnable {

        private final ArrayDeque<UnicastProcessor<T>> processors = new ArrayDeque<>();
        private final Supplier<? extends Queue<T>> supplier;

        private final Queue<UnicastProcessor<T>> overflow;
        private final int size;
        private final int skip;

        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final AtomicInteger count = new AtomicInteger();
        private final AtomicBoolean firstRequest = new AtomicBoolean();
        private final AtomicLong requested = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();

        private int index;
        private int produced;

        MultiWindowWithOverlapProcessor(MultiSubscriber<? super Multi<T>> downstream,
                int size, int skip,
                Supplier<? extends Queue<T>> supplier,
                Queue<UnicastProcessor<T>> overflowQueue) {
            super(downstream);
            this.size = size;
            this.skip = skip;
            this.supplier = supplier;
            count.lazySet(1);
            this.overflow = overflowQueue;
        }

        @Override
        public void onItem(T t) {
            if (isDone()) {
                return;
            }

            int i = index;
            if (i == 0) {
                if (!isCancelled()) {
                    count.getAndIncrement();
                    UnicastProcessor<T> proc = UnicastProcessor.create(supplier.get(), this);
                    processors.offer(proc);
                    overflow.offer(proc);
                    drain();
                }
            }

            i++;
            for (UnicastProcessor<T> proc : processors) {
                proc.onNext(t);
            }

            int p = produced + 1;
            if (p == size) {
                produced = p - skip;
                UnicastProcessor<T> proc = processors.poll();
                if (proc != null) {
                    proc.onComplete();
                }
            } else {
                produced = p;
            }

            if (i == skip) {
                index = 0;
            } else {
                index = i;
            }
        }

        @Override
        public void onFailure(Throwable f) {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                for (UnicastProcessor<T> proc : processors) {
                    proc.onError(f);
                }
                processors.clear();
                failure.set(f);
                drain();
            } else {
                Infrastructure.handleDroppedException(f);
            }
        }

        @Override
        public void onCompletion() {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                for (UnicastProcessor<T> proc : processors) {
                    proc.onComplete();
                }
                processors.clear();
                drain();
            }
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            final MultiSubscriber<? super Multi<T>> actual = downstream;
            final Queue<UnicastProcessor<T>> q = overflow;
            int missed = 1;

            for (;;) {
                long r = requested.get();
                long e = 0;

                while (e != r) {
                    boolean isDone = isDone();
                    UnicastProcessor<T> t = q.poll();
                    boolean isEmpty = t == null;
                    if (isCancelledOrDone(isDone, isEmpty, actual, q)) {
                        return;
                    }

                    if (isEmpty) {
                        break;
                    }

                    actual.onItem(t);

                    e++;
                }

                if (e == r) {
                    if (isCancelledOrDone(isDone(), q.isEmpty(), actual, q)) {
                        return;
                    }
                }

                if (e != 0L && r != Long.MAX_VALUE) {
                    requested.addAndGet(-e);
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        boolean isCancelledOrDone(boolean isDone, boolean isEmpty, Subscriber<?> subscriber, Queue<?> q) {
            if (isCancelled()) {
                q.clear();
                return true;
            }

            if (isDone) {
                Throwable failed = failure.get();

                if (failed != null) {
                    q.clear();
                    subscriber.onError(failed);
                    return true;
                } else if (isEmpty) {
                    subscriber.onComplete();
                    return true;
                }
            }

            return false;
        }

        @Override
        public void request(long n) {
            Subscriptions.add(requested, n);

            if (firstRequest.compareAndSet(false, true)) {
                long u = Subscriptions.multiply(skip, n - 1);
                long v = Subscriptions.add(size, u);
                super.request(v);
            } else {
                long u = Subscriptions.multiply(skip, n);
                super.request(u);
            }
            drain();
        }

        @Override
        public void cancel() {
            if (compareAndSwapDownstreamCancellationRequest()) {
                run();
            }
        }

        @Override
        public void run() {
            if (count.decrementAndGet() == 0) {
                getUpstreamSubscription().cancel();
            }
        }
    }
}
