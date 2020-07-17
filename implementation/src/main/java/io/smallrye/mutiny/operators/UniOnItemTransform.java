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

import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;

public class UniOnItemTransform<I, O> extends UniOperator<I, O> {

    private final Function<? super I, ? extends O> mapper;

    public UniOnItemTransform(Uni<I> source, Function<? super I, ? extends O> mapper) {
        super(ParameterValidation.nonNull(source, "source"));
        this.mapper = ParameterValidation.nonNull(mapper, "mapper");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<I, O>(subscriber) {

            @Override
            public void onItem(I item) {
                if (subscriber.isCancelledOrDone()) {
                    // Avoid calling the mapper if we are done to save some cycles.
                    // If the cancellation happen during the call, the events won't be dispatched.
                    return;
                }

                O outcome;
                try {
                    outcome = mapper.apply(item);
                    // We cannot call onItem here, as if onItem would throw an exception
                    // it would be caught and onFailure would be called. This would be illegal.
                } catch (Throwable e) {
                    subscriber.onFailure(e);
                    return;
                }

                subscriber.onItem(outcome);
            }

        });
    }
}
