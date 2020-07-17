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

import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiRepeatWhilstOp<T> extends AbstractMultiOperator<T, T> implements Multi<T> {
    private final Predicate<T> predicate;
    private final long times;

    public MultiRepeatWhilstOp(Multi<T> upstream, Predicate<T> predicate) {
        super(upstream);
        this.predicate = predicate;
        this.times = Long.MAX_VALUE;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        ParameterValidation.nonNullNpe(downstream, "downstream");
        RepeatWhilstProcessor<T> processor = new RepeatWhilstProcessor<>(upstream, downstream,
                times != Long.MAX_VALUE ? times - 1 : Long.MAX_VALUE,
                predicate);
        downstream.onSubscribe(processor);
        upstream.subscribe(processor);
    }

    static final class RepeatWhilstProcessor<T> extends MultiRepeatUntilOp.RepeatProcessor<T> {

        private boolean stop = false;

        public RepeatWhilstProcessor(Multi<? extends T> upstream, MultiSubscriber<? super T> downstream,
                long times, Predicate<T> predicate) {
            super(upstream, downstream, times, predicate);
        }

        @Override
        public void onItem(T t) {
            stop = !predicate.test(t);
            emitted++;
            downstream.onNext(t);
        }

        @Override
        public void onCompletion() {
            long r = remaining;
            if (r != Long.MAX_VALUE) {
                remaining = r - 1;
            }

            if (r != 0L && !stop) {
                subscribeNext();
            } else {
                downstream.onComplete();
            }
        }
    }

}
