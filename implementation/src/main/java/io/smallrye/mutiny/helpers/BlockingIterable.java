package io.smallrye.mutiny.helpers;

import static io.smallrye.mutiny.helpers.ParameterValidation.*;

import java.util.*;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.ContextSupport;

public class BlockingIterable<T> implements Iterable<T> {

    private final Multi<? extends T> upstream;
    private final Supplier<Queue<T>> supplier;
    private final int batchSize;
    private final Supplier<Context> contextSupplier;

    public BlockingIterable(Multi<? extends T> upstream, int batchSize, Supplier<Queue<T>> queueSupplier,
            Supplier<Context> contextSupplier) {
        this.upstream = nonNull(upstream, "upstream");
        this.batchSize = positive(batchSize, "batchSize");
        this.supplier = nonNull(queueSupplier, "queueSupplier");
        this.contextSupplier = nonNull(contextSupplier, "contextSupplier");
    }

    @Override
    public Iterator<T> iterator() {
        SubscriberIterator<T> iterator = create();
        Subscriber<? super T> actual = Infrastructure.onMultiSubscription(upstream, iterator);
        upstream.subscribe(actual);
        return iterator;
    }

    @Override
    public Spliterator<T> spliterator() {
        return stream().spliterator();
    }

    public Stream<T> stream() {
        SubscriberIterator<T> iterator = create();

        Spliterator<T> sp = Spliterators.spliteratorUnknownSize(iterator, 0);
        // On close cancel the subscription.
        Stream<T> stream = StreamSupport.stream(sp, false)
                .onClose(iterator::terminate);
        Subscriber<? super T> actual = Infrastructure.onMultiSubscription(upstream, iterator);
        Infrastructure.getDefaultExecutor().execute(() -> upstream.subscribe(actual));
        return stream;
    }

    private SubscriberIterator<T> create() {
        Queue<T> queue = null;
        // Create the instance of queue, check for failure and `null` values.
        try {
            queue = supplier.get();
        } catch (Throwable e) {
            propagateFailure(e);
        }

        if (queue == null) {
            throw new IllegalStateException(SUPPLIER_PRODUCED_NULL);
        }

        Context context = null;
        try {
            context = contextSupplier.get();
        } catch (Throwable e) {
            propagateFailure(e);
        }

        if (context == null) {
            throw new IllegalStateException(SUPPLIER_PRODUCED_NULL);
        }

        return new SubscriberIterator<>(queue, batchSize, context);
    }

    private static void propagateFailure(Throwable e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new RuntimeException(e);
        }
    }

    private static final class SubscriberIterator<T> implements Subscriber<T>, Iterator<T>, ContextSupport {

        private final Queue<T> queue;

        private final int batchSize;

        private final int limit;

        private final Lock lock;

        private final Condition condition;

        private final Context context;

        long produced;

        AtomicReference<Subscription> subscription = new AtomicReference<>();

        AtomicBoolean done = new AtomicBoolean();

        Throwable failure;

        SubscriberIterator(Queue<T> queue, int batchSize, Context context) {
            this.queue = queue;
            this.batchSize = batchSize;
            this.limit = batchSize;
            this.context = context;
            this.lock = new ReentrantLock();
            this.condition = lock.newCondition();
        }

        @Override
        public boolean hasNext() {

            while (true) {
                boolean actualDone = done.get();
                boolean empty = queue.isEmpty();

                // We are done, no more data.
                // We may have received a failure.
                if (actualDone) {
                    Throwable err = failure;
                    if (err != null) {
                        propagateFailure(err);
                        // exception thrown.
                    } else if (empty) {
                        return false;
                    }
                }

                // We are not done, check if empty, and block until we get data.
                if (empty) {
                    if (!Infrastructure.canCallerThreadBeBlocked()) {
                        throw new IllegalStateException(
                                "The current thread cannot be blocked: " + Thread.currentThread().getName());
                    }
                    lock.lock();
                    try {
                        while (!done.get() && queue.isEmpty()) {
                            condition.await();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        terminateAndFire();
                        propagateFailure(e);
                    } finally {
                        lock.unlock();
                    }
                    // Go to the next iteration, to get what happened (items, failure, completion)
                } else {
                    return true;
                }
            }
        }

        @Override
        public T next() {
            if (hasNext()) {
                T v = queue.poll();
                if (v == null) {
                    terminate();
                    propagateFailure(new IllegalArgumentException("`null` is not an accepted value"));
                }

                long numberOfProducedItems = produced + 1;
                if (numberOfProducedItems == limit) {
                    produced = 0;
                    subscription.get().request(numberOfProducedItems);
                } else {
                    produced = numberOfProducedItems;
                }

                return v;
            }
            // This is do be compliant with the spec of #next.
            throw new NoSuchElementException();
        }

        void fire() {
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }

        private void terminateAndFire() {
            terminate();
            fire();
        }

        private void terminate() {
            Subscription s = subscription.getAndSet(EmptyUniSubscription.CANCELLED);
            if (s != null) {
                s.cancel();
            }
        }

        @Override
        public Context context() {
            return this.context;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (subscription.compareAndSet(null, s)) {
                s.request(batchSize);
            }
        }

        @Override
        public void onNext(T t) {
            if (!queue.offer(t)) {
                subscription.getAndSet(EmptyUniSubscription.CANCELLED).cancel();
                onError(new BackPressureFailure("Buffer is full, cannot deliver the item"));
            } else {
                fire();
            }
        }

        @Override
        public void onError(Throwable t) {
            failure = t;
            done.set(true);
            fire();
        }

        @Override
        public void onComplete() {
            done.set(true);
            fire();
        }

    }
}
