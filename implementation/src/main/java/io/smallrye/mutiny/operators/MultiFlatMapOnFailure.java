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

import static io.smallrye.mutiny.helpers.ParameterValidation.MAPPER_RETURNED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOnFailureResumeOp;

public class MultiFlatMapOnFailure<T> extends MultiOperator<T, T> {
    private final Predicate<? super Throwable> predicate;
    private final Function<? super Throwable, ? extends Multi<? extends T>> mapper;

    public MultiFlatMapOnFailure(Multi<T> upstream, Predicate<? super Throwable> predicate,
            Function<? super Throwable, ? extends Multi<? extends T>> mapper) {
        super(nonNull(upstream, "upstream"));
        this.predicate = predicate == null ? x -> true : predicate;
        this.mapper = nonNull(mapper, "mapper");
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        Function<? super Throwable, ? extends Publisher<? extends T>> next = failure -> {
            if (predicate.test(failure)) {
                Publisher<? extends T> res = mapper.apply(failure);
                if (res == null) {
                    return Multi.createFrom().failure(new NullPointerException(MAPPER_RETURNED_NULL));
                } else {
                    return res;
                }
            }
            return Multi.createFrom().failure(failure);
        };
        Multi<T> op = Infrastructure.onMultiCreation(new MultiOnFailureResumeOp<>(upstream(), next));
        op.subscribe(subscriber);
    }
}
