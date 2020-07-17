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
import java.util.function.Predicate;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;

public class UniOnItemConsume<T> extends UniOperator<T, T> {

    private final Consumer<? super T> onItemCallback;
    private final Consumer<Throwable> onFailureCallback;
    private final Predicate<? super Throwable> onFailurePredicate;

    public UniOnItemConsume(Uni<? extends T> upstream,
            Consumer<? super T> onItemCallback,
            Consumer<Throwable> onFailureCallback, Predicate<? super Throwable> predicate) {
        super(upstream);
        this.onItemCallback = onItemCallback;
        this.onFailureCallback = onFailureCallback;
        this.onFailurePredicate = predicate;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onItem(T item) {
                if (invokeEventHandler(onItemCallback, item, false, subscriber)) {
                    subscriber.onItem(item);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (onFailurePredicate != null) {
                    try {
                        if (onFailurePredicate.test(failure)) {
                            if (invokeEventHandler(onFailureCallback, failure, true, subscriber)) {
                                subscriber.onFailure(failure);
                            }
                        } else {
                            subscriber.onFailure(failure);
                        }
                    } catch (Throwable e) {
                        subscriber.onFailure(new CompositeException(failure, e));
                    }
                } else {
                    if (invokeEventHandler(onFailureCallback, failure, true, subscriber)) {
                        subscriber.onFailure(failure);
                    }
                }
            }
        });
    }

    private <E> boolean invokeEventHandler(Consumer<? super E> handler, E event, boolean wasCalledByOnFailure,
            UniSerializedSubscriber<? super T> subscriber) {
        if (handler != null) {
            try {
                handler.accept(event);
            } catch (Throwable e) {
                if (wasCalledByOnFailure) {
                    subscriber.onFailure(new CompositeException((Throwable) event, e));
                } else {
                    subscriber.onFailure(e);
                }
                return false;
            }
        }
        return true;
    }
}
