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

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Executor;

import io.smallrye.mutiny.Uni;

public class UniEmitOn<I> extends UniOperator<I, I> {
    private final Executor executor;

    UniEmitOn(Uni<I> upstream, Executor executor) {
        super(upstream);
        this.executor = nonNull(executor, "executor");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<I, I>(subscriber) {
            @Override
            public void onItem(I item) {
                executor.execute(() -> subscriber.onItem(item));
            }

            @Override
            public void onFailure(Throwable failure) {
                executor.execute(() -> subscriber.onFailure(failure));
            }
        });
    }
}
