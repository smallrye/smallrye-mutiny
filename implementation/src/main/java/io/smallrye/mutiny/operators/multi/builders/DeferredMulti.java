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

import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class DeferredMulti<T> extends AbstractMulti<T> {
    private final Supplier<? extends Publisher<? extends T>> supplier;

    public DeferredMulti(Supplier<? extends Publisher<? extends T>> supplier) {
        this.supplier = ParameterValidation.nonNull(supplier, "supplier");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        Publisher<? extends T> publisher;
        try {
            publisher = supplier.get();
            if (publisher == null) {
                throw new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL);
            }
        } catch (Throwable failure) {
            Subscriptions.fail(downstream, failure);
            return;
        }
        publisher.subscribe(Infrastructure.onMultiSubscription(publisher, downstream));
    }
}
