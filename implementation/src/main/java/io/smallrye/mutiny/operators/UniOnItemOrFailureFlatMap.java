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
import static io.smallrye.mutiny.operators.UniOnItemTransformToUni.handleInnerSubscription;

import java.util.function.BiFunction;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnItemOrFailureFlatMap<I, O> extends UniOperator<I, O> {

    private final BiFunction<? super I, Throwable, ? extends Uni<? extends O>> mapper;

    public UniOnItemOrFailureFlatMap(Uni<I> upstream,
            BiFunction<? super I, Throwable, ? extends Uni<? extends O>> mapper) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "mapper");
    }

    public static <I, O> void invokeAndSubstitute(BiFunction<? super I, Throwable, ? extends Uni<? extends O>> mapper,
            I item,
            Throwable failure,
            UniSerializedSubscriber<? super O> subscriber,
            UniOnItemTransformToUni.FlatMapSubscription flatMapSubscription) {
        Uni<? extends O> outcome;
        try {
            outcome = mapper.apply(item, failure);
            // We cannot call onItem here, as if onItem would throw an exception
            // it would be caught and onFailure would be called. This would be illegal.
        } catch (Throwable e) { // NOSONAR
            if (failure != null) {
                subscriber.onFailure(new CompositeException(failure, e));
            } else {
                subscriber.onFailure(e);
            }
            return;
        }

        handleInnerSubscription(subscriber, flatMapSubscription, outcome);
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        UniOnItemTransformToUni.FlatMapSubscription flatMapSubscription = new UniOnItemTransformToUni.FlatMapSubscription();
        // Subscribe to the source.
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<I, O>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                flatMapSubscription.setInitialUpstream(subscription);
                subscriber.onSubscribe(flatMapSubscription);
            }

            @Override
            public void onItem(I item) {
                invokeAndSubstitute(mapper, item, null, subscriber, flatMapSubscription);
            }

            @Override
            public void onFailure(Throwable failure) {
                invokeAndSubstitute(mapper, null, failure, subscriber, flatMapSubscription);
            }
        });
    }
}
