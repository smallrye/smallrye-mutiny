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
package io.smallrye.mutiny.operators.multi.overflow;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.SpscArrayQueue;
import io.smallrye.mutiny.helpers.queues.SpscLinkedArrayQueue;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnOverflowBufferOp<T> extends AbstractMultiOperator<T, T> {

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
    public void subscribe(MultiSubscriber<? super T> downstream) {
        OnOverflowBufferProcessor<T> subscriber = new OnOverflowBufferProcessor<>(downstream,
                bufferSize, unbounded,
                postponeFailurePropagation,
                onOverflow);
        upstream.subscribe().withSubscriber(subscriber);
    }

    static final class OnOverflowBufferProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final Queue<T> queue;
        private final boolean postponeFailurePropagation;
        private final Consumer<T> onOverflow;

        Throwable failure;

        private final AtomicLong requested = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();

        volatile boolean cancelled;
        volatile boolean done;

        OnOverflowBufferProcessor(MultiSubscriber<? super T> downstream, int bufferSize,
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
        public void onItem(T t) {
            if (!queue.offer(t)) {
                BackPressureFailure ex = new BackPressureFailure(
                        "Buffer is full due to lack of downstream consumption");
                try {
                    onOverflow.accept(t);
                } catch (Throwable e) {
                    ex.initCause(e);
                }
                onFailure(ex);
                return;
            }
            drain();
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
                if (postponeFailurePropagation) {
                    if (wasEmpty) {
                        if (failure != null) {
                            super.onFailure(failure);
                        } else {
                            super.onCompletion();
                        }
                        return true;
                    }
                } else {
                    if (failure != null) {
                        queue.clear();
                        super.onFailure(failure);
                        return true;
                    } else if (wasEmpty) {
                        super.onCompletion();
                        return true;
                    }
                }
            }
            return false;
        }
    }

}
