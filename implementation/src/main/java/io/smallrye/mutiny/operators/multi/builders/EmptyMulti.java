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

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Implements a {@link org.reactivestreams.Publisher} which only calls {@code onComplete} immediately after subscription.
 */
public final class EmptyMulti extends AbstractMulti<Object> {

    private static final Multi<Object> EMPTY = new EmptyMulti();

    private EmptyMulti() {
        // avoid direct instantiation
    }

    @SuppressWarnings("unchecked")
    public static <T> Multi<T> empty() {
        return (Multi<T>) EMPTY;
    }

    @Override
    public void subscribe(MultiSubscriber<? super Object> downstream) {
        ParameterValidation.nonNullNpe(downstream, "downstream");
        Subscriptions.complete(downstream);
    }

}
