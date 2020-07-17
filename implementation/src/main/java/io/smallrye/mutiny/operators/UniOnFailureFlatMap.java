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

import java.util.function.Function;
import java.util.function.Predicate;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnFailureFlatMap<I> extends UniOperator<I, I> {

    private final Function<? super Throwable, ? extends Uni<? extends I>> mapper;
    private final Predicate<? super Throwable> predicate;

    public UniOnFailureFlatMap(Uni<I> upstream,
            Predicate<? super Throwable> predicate,
            Function<? super Throwable, ? extends Uni<? extends I>> mapper) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "mapper");
        this.predicate = nonNull(predicate, "predicate");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        UniOnItemTransformToUni.FlatMapSubscription flatMapSubscription = new UniOnItemTransformToUni.FlatMapSubscription();
        // Subscribe to the source.
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<I, I>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                flatMapSubscription.setInitialUpstream(subscription);
                subscriber.onSubscribe(flatMapSubscription);
            }

            @Override
            public void onFailure(Throwable failure) {
                boolean test;
                try {
                    test = predicate.test(failure);
                } catch (RuntimeException e) {
                    subscriber.onFailure(new CompositeException(failure, e));
                    return;
                }

                if (test) {
                    UniOnItemTransformToUni.invokeAndSubstitute(mapper, failure, subscriber, flatMapSubscription);
                } else {
                    subscriber.onFailure(failure);
                }

            }

        });
    }
}
