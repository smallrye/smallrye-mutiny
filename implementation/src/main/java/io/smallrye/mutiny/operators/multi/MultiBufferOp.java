/*
 * Copyright (c) 2011-2018 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.DrainUtils;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Buffers a given number of items and emits the <em>groups</em> as a single item downstream.
 * This implementation uses {@link java.util.ArrayList} and so emits {@link List}.
 *
 * @param <T> the type of item from upstream
 */
public class MultiBufferOp<T> extends AbstractMultiOperator<T, List<T>> {

    private final int size;

    private final int skip;

    private final Supplier<List<T>> supplier;

    public MultiBufferOp(Multi<? extends T> upstream, int size, int skip) {
        super(upstream);
        this.size = ParameterValidation.positive(size, "size");
        this.skip = ParameterValidation.positive(skip, "skip");
        this.supplier = () -> new ArrayList<>(size);
    }

    @Override
    public void subscribe(MultiSubscriber<? super List<T>> downstream) {
        if (size == skip) {
            upstream.subscribe().withSubscriber(new BufferExactProcessor<>(downstream, size, supplier));
        } else if (skip > size) {
            upstream.subscribe().withSubscriber(new BufferSkipProcessor<>(downstream, size, skip, supplier));
        } else {
            upstream.subscribe().withSubscriber(new BufferOverlappingProcessor<>(downstream,
                    size,
                    skip,
                    supplier));
        }
    }

    static final class BufferExactProcessor<T> extends MultiOperatorProcessor<T, List<T>> {

        private final Supplier<List<T>> supplier;
        private final int size;
        private List<T> current;

        BufferExactProcessor(MultiSubscriber<? super List<T>> downstream, int size, Supplier<List<T>> supplier) {
            super(downstream);
            this.size = size;
            this.supplier = supplier;
        }

        @Override
        public void request(long n) {
            Subscription subscription = getUpstreamSubscription();
            if (subscription != CANCELLED) {
                subscription.request(Subscriptions.multiply(n, size));
            }
        }

        @Override
        public void onItem(T t) {
            if (isDone()) {
                return;
            }

            if (current == null) {
                current = supplier.get();
            }

            current.add(t);
            if (current.size() == size) {
                List<T> buffer = current;
                current = null;
                downstream.onItem(buffer);
            }
        }

        @Override
        public void onCompletion() {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                List<T> buffer = current;
                if (buffer != null && !buffer.isEmpty()) {
                    downstream.onItem(buffer);
                }
                downstream.onCompletion();
            }
        }
    }

    static final class BufferSkipProcessor<T> extends MultiOperatorProcessor<T, List<T>> {

        private final Supplier<List<T>> supplier;
        private final int size;
        private final int skip;
        private List<T> current;

        private long index;

        private final AtomicInteger wip = new AtomicInteger();

        BufferSkipProcessor(MultiSubscriber<? super List<T>> downstream, int size, int skip,
                Supplier<List<T>> supplier) {
            super(downstream);
            this.size = size;
            this.skip = skip;
            this.supplier = supplier;
        }

        @Override
        public void request(long n) {
            if (wip.compareAndSet(0, 1)) {
                // n full buffers
                long u = Subscriptions.multiply(n, size);
                // + (n - 1) gaps
                long v = Subscriptions.multiply(skip - (long) size, n - 1);
                super.request(Subscriptions.add(u, v));
            } else {
                // n full buffer + gap
                super.request(Subscriptions.multiply(skip, n));
            }
        }

        @Override
        public void onItem(T item) {
            if (isDone()) {
                return;
            }

            List<T> buffer = current;
            long i = index;
            if (i % skip == 0L) {
                buffer = supplier.get();
                current = buffer;
            }

            if (buffer != null) {
                buffer.add(item);
                if (buffer.size() == size) {
                    current = null;
                    downstream.onItem(buffer);
                }
            }
            index = i + 1;
        }

        @Override
        public void onFailure(Throwable t) {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                current = null;
                downstream.onFailure(t);
            } else {
                Infrastructure.handleDroppedException(t);
            }
        }

        @Override
        public void onCompletion() {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                List<T> buffer = current;
                current = null;
                if (buffer != null) {
                    downstream.onItem(buffer);
                }
                downstream.onCompletion();
            }
        }
    }

    static final class BufferOverlappingProcessor<T> extends MultiOperatorProcessor<T, List<T>> {

        private final Supplier<List<T>> supplier;
        private final int size;
        private final int skip;

        long index;
        long produced;

        private final AtomicBoolean firstRequest = new AtomicBoolean();
        private final AtomicLong requested = new AtomicLong();
        private final ArrayDeque<List<T>> queue = new ArrayDeque<>();

        BufferOverlappingProcessor(MultiSubscriber<? super List<T>> downstream, int size, int skip,
                Supplier<List<T>> supplier) {
            super(downstream);
            this.size = size;
            this.skip = skip;
            this.supplier = supplier;
        }

        @Override
        public void request(long n) {
            if (DrainUtils.postCompleteRequest(n,
                    downstream,
                    queue,
                    requested,
                    this::isCancelled)) {
                return;
            }

            if (firstRequest.compareAndSet(false, true)) {
                // (n - 1) skips
                long u = Subscriptions.multiply(skip, n - 1);
                // + 1 full buffer
                long r = Subscriptions.add(size, u);
                super.request(r);
            } else {
                // n skips
                long r = Subscriptions.multiply(skip, n);
                super.request(r);
            }
        }

        @Override
        public void onItem(T item) {
            if (isDone()) {
                return;
            }

            long i = index;

            if (i % skip == 0L) {
                List<T> b = supplier.get();
                queue.offer(b);
            }

            List<T> b = queue.peek();

            if (b != null && b.size() + 1 == size) {
                queue.poll();
                b.add(item);
                downstream.onItem(b);
                produced++;
            }

            for (List<T> l : queue) {
                l.add(item);
            }

            index = i + 1;
        }

        @Override
        public void onFailure(Throwable t) {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                downstream.onFailure(t);
            } else {
                Infrastructure.handleDroppedException(t);
            }
        }

        @Override
        public void onCompletion() {
            Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
            if (subscription != CANCELLED) {
                long p = produced;
                if (p != 0L) {
                    Subscriptions.produced(requested, p);
                }
                DrainUtils.postComplete(downstream, queue, requested, this::isCancelled);
            }
        }
    }
}
