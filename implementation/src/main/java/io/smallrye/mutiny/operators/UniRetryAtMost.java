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

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.Predicates;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniRetryAtMost<T> extends UniOperator<T, T> {
    private final Predicate<? super Throwable> predicate;
    private final long maxAttempts;

    public UniRetryAtMost(Uni<T> upstream, Predicate<? super Throwable> predicate, long maxAttempts) {
        super(nonNull(upstream, "upstream"));
        this.predicate = nonNull(predicate, "predicate");
        this.maxAttempts = positive(maxAttempts, "maxAttempts");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        AtomicInteger numberOfSubscriptions = new AtomicInteger(0);
        UniSubscriber<T> retryingSubscriber = new UniSubscriber<T>() {
            AtomicReference<UniSubscription> reference = new AtomicReference<>();

            @Override
            public void onSubscribe(UniSubscription subscription) {
                if (numberOfSubscriptions.getAndIncrement() == 0) {
                    subscriber.onSubscribe(() -> {
                        UniSubscription old = reference.getAndSet(CANCELLED);
                        if (old != null) {
                            old.cancel();
                        }
                    });
                } else {
                    reference.compareAndSet(null, subscription);
                }
            }

            @Override
            public void onItem(T item) {
                if (reference.get() != CANCELLED) {
                    subscriber.onItem(item);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                if (reference.get() != CANCELLED) {
                    if (!Predicates.testFailure(predicate, subscriber, failure)) {
                        return;
                    }

                    if (numberOfSubscriptions.get() > maxAttempts) {
                        subscriber.onFailure(failure);
                        return;
                    }

                    // retry.
                    UniSubscription old = reference.getAndSet(null);
                    if (old != null) {
                        old.cancel();
                    }
                    resubscribe(upstream(), this);
                }
            }
        };

        AbstractUni.subscribe(upstream(), retryingSubscriber);
    }

    private void resubscribe(Uni<? extends T> upstream, UniSubscriber<T> subscriber) {
        AbstractUni.subscribe(upstream, subscriber);
    }
}
