/*
 * Copyright (c) 2019-2020 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.smallrye.mutiny.helpers;

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.infrastructure.Infrastructure;

public class BlockingIterable<T> implements Iterable<T> {

    private final Publisher<? extends T> upstream;
    private final Supplier<Queue<T>> supplier;
    private final int batchSize;

    public BlockingIterable(Publisher<? extends T> upstream, int batchSize, Supplier<Queue<T>> supplier) {
        this.upstream = nonNull(upstream, "upstream");
        this.batchSize = positive(batchSize, "batchSize");
        this.supplier = nonNull(supplier, "supplier");
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

        return new SubscriberIterator<>(queue, batchSize);
    }

    private static void propagateFailure(Throwable e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("SubscriberImplementation")
    private static final class SubscriberIterator<T> implements Subscriber<T>, Iterator<T> {

        private final Queue<T> queue;

        private final int batchSize;

        private final int limit;

        private final Lock lock;

        private final Condition condition;

        long produced;

        AtomicReference<Subscription> subscription = new AtomicReference<>();

        AtomicBoolean done = new AtomicBoolean();

        Throwable failure;

        SubscriberIterator(Queue<T> queue, int batchSize) {
            this.queue = queue;
            this.batchSize = batchSize;
            this.limit = batchSize;
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
                    // TODO Must be sure we are not on an IO Thread here.
                    lock.lock();
                    try {
                        while (!done.get() && queue.isEmpty()) {
                            condition.await();
                        }
                    } catch (InterruptedException intex) {
                        Thread.currentThread().interrupt();
                        terminateAndFire();
                        propagateFailure(intex);
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
        public void onSubscribe(Subscription s) {
            if (subscription.compareAndSet(null, s)) {
                s.request(batchSize);
            }
        }

        @Override
        public void onNext(T t) {
            if (!queue.offer(t)) {
                subscription.getAndSet(EmptyUniSubscription.CANCELLED).cancel();
                onError(new IllegalStateException("Buffer is full, cannot deliver the item"));
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
