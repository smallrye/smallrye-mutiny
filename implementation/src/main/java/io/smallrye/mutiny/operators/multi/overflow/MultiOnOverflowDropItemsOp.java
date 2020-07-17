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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnOverflowDropItemsOp<T> extends AbstractMultiOperator<T, T> {

    private final Consumer<? super T> onItemDrop;

    public MultiOnOverflowDropItemsOp(Multi<T> upstream) {
        super(upstream);
        this.onItemDrop = null;
    }

    public MultiOnOverflowDropItemsOp(Multi<T> upstream, Consumer<? super T> onItemDrop) {
        super(upstream);
        this.onItemDrop = onItemDrop;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiOnOverflowDropItemsProcessor<T>(downstream, onItemDrop));
    }

    static final class MultiOnOverflowDropItemsProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final Consumer<? super T> onItemDrop;
        private final AtomicLong requested = new AtomicLong();

        MultiOnOverflowDropItemsProcessor(MultiSubscriber<? super T> downstream, Consumer<? super T> onItemDrop) {
            super(downstream);
            this.onItemDrop = onItemDrop;
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
        public void onItem(T item) {
            if (isDone()) {
                return;
            }
            long req = requested.get();
            if (req != 0L) {
                downstream.onItem(item);
                Subscriptions.subtract(requested, 1);
            } else {
                // no request, dropping.
                drop(item);
            }
        }

        private void drop(T item) {
            if (onItemDrop != null) {
                try {
                    onItemDrop.accept(item);
                } catch (Throwable e) {
                    onFailure(e);
                }
            }
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
            }
        }
    }
}
