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

import java.util.function.BiConsumer;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;

public class UniOnItemOrFailureConsume<T> extends UniOperator<T, T> {

    private final BiConsumer<? super T, Throwable> callback;

    public UniOnItemOrFailureConsume(Uni<? extends T> upstream,
            BiConsumer<? super T, Throwable> callback) {
        super(upstream);
        this.callback = callback;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onItem(T item) {
                if (invokeCallback(item, null, subscriber)) {
                    subscriber.onItem(item);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (invokeCallback(null, failure, subscriber)) {
                    subscriber.onFailure(failure);
                }
            }
        });
    }

    private boolean invokeCallback(T item, Throwable failure, UniSerializedSubscriber<? super T> subscriber) {
        try {
            callback.accept(item, failure);
            return true;
        } catch (Throwable e) {
            if (failure != null) {
                subscriber.onFailure(new CompositeException(failure, e));
            } else {
                subscriber.onFailure(e);
            }
            return false;
        }
    }
}
