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

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnCancellation<T> extends UniOperator<T, T> {
    private final Runnable callback;

    public UniOnCancellation(Uni<T> upstream, Runnable callback) {
        super(nonNull(upstream, "upstream"));
        this.callback = nonNull(callback, "callback");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                super.onSubscribe(() -> {
                    callback.run();
                    subscription.cancel();
                });
            }
        });
    }
}
