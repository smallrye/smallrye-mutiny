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

import java.util.function.Consumer;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnSubscribeInvoke<T> extends UniOperator<T, T> {

    private final Consumer<? super UniSubscription> callback;

    public UniOnSubscribeInvoke(Uni<? extends T> upstream,
            Consumer<? super UniSubscription> callback) {
        super(ParameterValidation.nonNull(upstream, "upstream"));
        this.callback = callback;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                // Invoke callback
                try {
                    callback.accept(subscription);
                } catch (Throwable e) {
                    super.onSubscribe(EmptyUniSubscription.CANCELLED);
                    super.onFailure(e);
                    return;
                }

                // Pass the subscription downstream
                // Cannot be done in the try block as it may propagates 2 subscriptions.
                super.onSubscribe(subscription);
            }
        });
    }
}
