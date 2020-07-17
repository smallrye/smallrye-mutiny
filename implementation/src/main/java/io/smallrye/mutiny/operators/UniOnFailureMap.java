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

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Function;
import java.util.function.Predicate;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;

public class UniOnFailureMap<I, O> extends UniOperator<I, O> {

    private final Function<? super Throwable, ? extends Throwable> mapper;
    private final Predicate<? super Throwable> predicate;

    public UniOnFailureMap(Uni<I> upstream,
            Predicate<? super Throwable> predicate,
            Function<? super Throwable, ? extends Throwable> mapper) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "mapper");
        this.predicate = nonNull(predicate, "predicate");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<I, O>(subscriber) {

            @Override
            public void onFailure(Throwable failure) {
                if (subscriber.isCancelledOrDone()) {
                    // Avoid calling the mapper if we are done to save some cycles.
                    // If the cancellation happen during the call, the events won't be dispatched.
                    return;
                }
                boolean test;
                try {
                    test = predicate.test(failure);
                } catch (RuntimeException e) {
                    subscriber.onFailure(new CompositeException(failure, e));
                    return;
                }

                if (test) {
                    Throwable outcome;
                    try {
                        outcome = mapper.apply(failure);
                        // We cannot call onFailure here, as if onFailure would throw an exception
                        // it would be caught and onFailure would be called. This would be illegal.
                    } catch (Throwable e) {
                        subscriber.onFailure(e);
                        return;
                    }
                    if (outcome == null) {
                        subscriber.onFailure(new NullPointerException(SUPPLIER_PRODUCED_NULL));
                    } else {
                        subscriber.onFailure(outcome);
                    }
                } else {
                    subscriber.onFailure(failure);
                }
            }

        });
    }
}
