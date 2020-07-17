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

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnTerminationInvoke<T> extends AbstractMultiOperator<T, T> {

    private final BiConsumer<Throwable, Boolean> callback;

    public MultiOnTerminationInvoke(Multi<? extends T> upstream, BiConsumer<Throwable, Boolean> callback) {
        super(nonNull(upstream, "upstream"));
        this.callback = nonNull(callback, "callback");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiOnTerminationInvokeProcessor(nonNull(downstream, "downstream")));
    }

    class MultiOnTerminationInvokeProcessor extends MultiOperatorProcessor<T, T> {

        private final AtomicBoolean actionInvoke = new AtomicBoolean();

        public MultiOnTerminationInvokeProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onFailure(Throwable failure) {
            try {
                execute(failure, false);
                super.onFailure(failure);
            } catch (Throwable err) {
                super.onFailure(new CompositeException(failure, err));
            }
        }

        @Override
        public void onItem(T item) {
            downstream.onItem(item);
        }

        @Override
        public void onCompletion() {
            try {
                execute(null, false);
                super.onCompletion();
            } catch (Throwable err) {
                super.onFailure(err);
            }
        }

        @Override
        public void cancel() {
            try {
                execute(null, true);
            } catch (Throwable ignored) {
                // TODO this exception is being swallowed
            }
            super.cancel();
        }

        private void execute(Throwable err, Boolean cancelled) {
            if (actionInvoke.compareAndSet(false, true)) {
                callback.accept(err, cancelled);
            }
        }
    }
}
