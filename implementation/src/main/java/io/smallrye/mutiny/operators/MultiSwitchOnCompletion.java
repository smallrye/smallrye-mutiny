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
package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiConcatOp;

public class MultiSwitchOnCompletion<T> extends MultiOperator<T, T> {
    private final Supplier<Publisher<? extends T>> supplier;

    public MultiSwitchOnCompletion(Multi<T> upstream, Supplier<Publisher<? extends T>> supplier) {
        super(upstream);
        this.supplier = supplier;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        Publisher<T> followup = Multi.createFrom().deferred(() -> {
            Publisher<? extends T> publisher;
            try {
                publisher = supplier.get();
            } catch (Throwable e) {
                return Multi.createFrom().failure(e);
            }
            if (publisher == null) {
                return Multi.createFrom().failure(new NullPointerException(SUPPLIER_PRODUCED_NULL));
            }
            if (publisher instanceof Multi) {
                //noinspection unchecked
                return (Multi<T>) publisher;
            } else {
                return Multi.createFrom().publisher(publisher);
            }
        });

        @SuppressWarnings("unchecked")
        Publisher<T>[] publishers = new Publisher[] { upstream(), followup };
        Multi<T> op = Infrastructure.onMultiCreation(new MultiConcatOp<>(false, publishers));
        op.subscribe(subscriber);
    }
}
