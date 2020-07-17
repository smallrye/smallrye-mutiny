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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniOrCombination<T> extends UniOperator<Void, T> {

    private final List<Uni<? super T>> challengers;

    public UniOrCombination(Iterable<? extends Uni<? super T>> iterable) {
        super(null);
        this.challengers = new ArrayList<>();
        nonNull(iterable, "produceIterable")
                .forEach(u -> challengers.add(nonNull(u, "iterable` must not contain a `null` value")));
    }

    public UniOrCombination(Uni<? super T>[] array) {
        super(null);
        nonNull(array, "array");
        this.challengers = new ArrayList<>();
        for (Uni<? super T> u : array) {
            challengers.add(nonNull(u, "array` must not contain a `null` value"));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        if (challengers.isEmpty()) {
            subscriber.onSubscribe(EmptyUniSubscription.CANCELLED);
            subscriber.onItem(null);
            return;
        }

        if (challengers.size() == 1) {
            // Just subscribe to the first and unique uni.
            Uni<? super T> uni = challengers.get(0);
            AbstractUni.subscribe(uni, (UniSubscriber) subscriber);
            return;
        }

        // Barrier - once set to {@code true} the signals are ignored
        AtomicBoolean completedOrCancelled = new AtomicBoolean();

        List<CompletableFuture<? super T>> futures = new ArrayList<>();
        challengers.forEach(uni -> {
            CompletableFuture<? super T> future = uni.subscribe().asCompletionStage();
            futures.add(future);
        });

        // Do not call unSubscribe until we get all the futures.
        // But at the same time we can't start resolving as we didn't give a subscription to the subscriber
        // yet.

        subscriber.onSubscribe(() -> {
            if (completedOrCancelled.compareAndSet(false, true)) {
                // Cancel all
                futures.forEach(cf -> cf.cancel(false));
            }
        });

        // Once the subscription has been given, start resolving
        futures.forEach(future -> future.whenComplete((res, fail) -> {
            if (completedOrCancelled.compareAndSet(false, true)) {
                // Cancel other
                futures.forEach(cf -> {
                    if (cf != future) {
                        cf.cancel(false);
                    }
                });
                if (fail != null) {
                    subscriber.onFailure(fail);
                } else {
                    subscriber.onItem((T) res);
                }
            }
        }));
    }
}
