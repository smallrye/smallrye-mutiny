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
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniDelayOnItem<T> extends UniOperator<T, T> {
    private final Duration duration;
    private final ScheduledExecutorService executor;

    public UniDelayOnItem(Uni<T> upstream, Duration duration, ScheduledExecutorService executor) {
        super(nonNull(upstream, "upstream"));
        this.duration = validate(duration, "duration");
        this.executor = nonNull(executor, "executor");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        AtomicReference<ScheduledFuture<?>> holder = new AtomicReference<>();
        AtomicReference<UniSubscription> reference = new AtomicReference<>();
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<T, T>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                if (reference.compareAndSet(null, subscription)) {
                    super.onSubscribe(() -> {
                        if (reference.compareAndSet(subscription, CANCELLED)) {
                            subscription.cancel();
                            ScheduledFuture<?> future = holder.getAndSet(null);
                            if (future != null) {
                                future.cancel(true);
                            }
                        }
                    });
                }
            }

            @Override
            public void onItem(T item) {
                if (reference.get() != CANCELLED) {
                    try {
                        ScheduledFuture<?> future = executor
                                .schedule(() -> super.onItem(item), duration.toMillis(), TimeUnit.MILLISECONDS);
                        holder.set(future);
                    } catch (RuntimeException e) {
                        // Typically, a rejected execution exception
                        super.onFailure(e);
                    }
                }
            }
        });
    }
}
