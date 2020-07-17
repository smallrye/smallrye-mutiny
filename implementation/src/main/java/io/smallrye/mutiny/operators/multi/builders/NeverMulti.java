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
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Represents a publisher that does not emits any signals (items, failures or completion).
 */
public final class NeverMulti extends AbstractMulti<Object> {

    private static final Publisher<Object> NEVER = new NeverMulti();

    /**
     * Returns a parameterized never of this never Publisher.
     *
     * @param <T> the value type
     * @return the {@code multi}
     */
    public static <T> Multi<T> never() {
        return (Multi<T>) NEVER;
    }

    private NeverMulti() {
        // avoid direct instantiation
    }

    @Override
    public void subscribe(MultiSubscriber<? super Object> actual) {
        actual.onSubscribe(Subscriptions.CANCELLED);
    }

}
