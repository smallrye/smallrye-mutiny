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
package io.smallrye.mutiny.operators.multi.builders;

import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.helpers.queues.SpscLinkedArrayQueue;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class BufferItemMultiEmitter<T> extends BaseMultiEmitter<T> {

    private final SpscLinkedArrayQueue<T> queue;
    private Throwable failure;
    private volatile boolean done;
    private final AtomicInteger wip = new AtomicInteger();

    BufferItemMultiEmitter(MultiSubscriber<? super T> actual, int capacityHint) {
        super(actual);
        this.queue = new SpscLinkedArrayQueue<>(capacityHint);
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
        final SpscLinkedArrayQueue<T> q = queue;

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
