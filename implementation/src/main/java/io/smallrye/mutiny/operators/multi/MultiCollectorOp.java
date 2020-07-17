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
package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public final class MultiCollectorOp<T, A, R> extends AbstractMultiOperator<T, R> {

    private final Collector<? super T, A, ? extends R> collector;
    private final boolean acceptNullAsInitialValue;

    public MultiCollectorOp(Multi<T> upstream, Collector<? super T, A, ? extends R> collector,
            boolean acceptNullAsInitialValue) {
        super(upstream);
        this.collector = collector;
        this.acceptNullAsInitialValue = acceptNullAsInitialValue;
    }

    @Override
    public void subscribe(MultiSubscriber<? super R> downstream) {
        A initialValue;
        BiConsumer<A, ? super T> accumulator;
        Function<A, ? extends R> finisher;

        try {
            initialValue = collector.supplier().get();
            accumulator = collector.accumulator();
            finisher = collector.finisher();
        } catch (Throwable ex) {
            Subscriptions.fail(downstream, ex, upstream);
            return;
        }

        if (initialValue == null && !acceptNullAsInitialValue) {
            Subscriptions.fail(downstream, new NullPointerException(SUPPLIER_PRODUCED_NULL), upstream);
            return;
        }

        if (accumulator == null) {
            Subscriptions.fail(downstream, new NullPointerException("`accumulator` must not be `null`"), upstream);
            return;
        }

        CollectorProcessor<? super T, A, ? extends R> processor = new CollectorProcessor<>(downstream, initialValue,
                accumulator, finisher);
        upstream.subscribe().withSubscriber(processor);
    }

    static class CollectorProcessor<T, A, R> extends MultiOperatorProcessor<T, R> {

        private final BiConsumer<A, T> accumulator;
        private final Function<A, R> finisher;
        // Only accessed in the serialized callbacks
        private A intermediate;

        CollectorProcessor(MultiSubscriber<? super R> downstream,
                A initialValue, BiConsumer<A, T> accumulator, Function<A, R> finisher) {
            super(downstream);
            this.intermediate = initialValue;
            this.accumulator = accumulator;
            this.finisher = finisher;
        }

        @Override
        public void onItem(T item) {
            if (upstream.get() != CANCELLED) {
                try {
                    accumulator.accept(intermediate, item);
                } catch (Throwable ex) {
                    failAndCancel(ex);
                }
            }
        }

        @Override
        public void onCompletion() {
            Subscription subscription = upstream.getAndSet(Subscriptions.CANCELLED);
            if (subscription != Subscriptions.CANCELLED) {
                R result;

                try {
                    result = finisher.apply(intermediate);
                } catch (Throwable ex) {
                    downstream.onFailure(ex);
                    return;
                }

                intermediate = null;
                if (result != null) {
                    downstream.onItem(result);
                }
                downstream.onCompletion();
            }
        }

        @Override
        public void request(long n) {
            // The subscriber may request only 1 but as we don't know how much we get, we request MAX.
            // This could be changed with call to request in the OnNext/OnItem
            super.request(Long.MAX_VALUE);
        }

    }
}
