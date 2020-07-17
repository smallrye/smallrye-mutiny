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

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Multi emitting a failures (constant or produced by a supplier) to subscribers.
 *
 * @param <T> the value type
 */
public class FailedMulti<T> extends AbstractMulti<T> {

    private final Supplier<Throwable> supplier;

    public FailedMulti(Throwable failure) {
        ParameterValidation.nonNull(failure, "failure");
        this.supplier = () -> failure;
    }

    public FailedMulti(Supplier<Throwable> supplier) {
        ParameterValidation.nonNull(supplier, "supplier");
        this.supplier = supplier;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        try {
            Throwable throwable = supplier.get();
            if (throwable == null) {
                Subscriptions.fail(actual, new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL));
            } else {
                Subscriptions.fail(actual, throwable);
            }
        } catch (Throwable e) {
            Subscriptions.fail(actual, e);
        }

    }

}
