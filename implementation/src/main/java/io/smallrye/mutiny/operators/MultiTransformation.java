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

import java.time.Duration;
import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiDistinctOp;
import io.smallrye.mutiny.operators.multi.MultiDistinctUntilChangedOp;
import io.smallrye.mutiny.operators.multi.MultiSkipLastOp;
import io.smallrye.mutiny.operators.multi.MultiSkipOp;
import io.smallrye.mutiny.operators.multi.MultiSkipUntilOp;
import io.smallrye.mutiny.operators.multi.MultiSkipUntilPublisherOp;
import io.smallrye.mutiny.operators.multi.MultiTakeLastOp;
import io.smallrye.mutiny.operators.multi.MultiTakeOp;
import io.smallrye.mutiny.operators.multi.MultiTakeUntilOtherOp;
import io.smallrye.mutiny.operators.multi.MultiTakeWhileOp;

public class MultiTransformation {

    private MultiTransformation() {
        // avoid direct instantiation
    }

    public static <T> Multi<T> skipFirst(Multi<T> upstream, long number) {
        return Infrastructure.onMultiCreation(new MultiSkipOp<>(upstream, number));
    }

    public static <T> Multi<T> skipLast(Multi<T> upstream, int number) {
        return Infrastructure.onMultiCreation(new MultiSkipLastOp<>(upstream, number));
    }

    public static <T> Multi<T> skipForDuration(Multi<T> upstream, Duration duration) {
        Multi<Long> ticks = Multi.createFrom().ticks().startingAfter(duration).every(duration);
        return Infrastructure.onMultiCreation(new MultiSkipUntilPublisherOp<>(upstream, ticks));
    }

    public static <T> Multi<T> skipWhile(Multi<T> upstream, Predicate<? super T> predicate) {
        return Infrastructure.onMultiCreation(new MultiSkipUntilOp<>(upstream, predicate));
    }

    public static <T> Multi<T> takeFirst(Multi<T> upstream, long number) {
        return Infrastructure.onMultiCreation(new MultiTakeOp<>(upstream, number));
    }

    public static <T> Multi<T> takeLast(Multi<T> upstream, int number) {
        return Infrastructure.onMultiCreation(new MultiTakeLastOp<>(upstream, number));
    }

    public static <T> Multi<T> takeForDuration(Multi<T> upstream, Duration duration) {
        Multi<Long> ticks = Multi.createFrom().ticks().startingAfter(duration).every(duration);
        return Infrastructure.onMultiCreation(new MultiTakeUntilOtherOp<>(upstream, ticks));
    }

    public static <T> Multi<T> takeWhile(Multi<T> upstream, Predicate<? super T> predicate) {
        return Infrastructure.onMultiCreation(new MultiTakeWhileOp<>(upstream, predicate));
    }

    public static <T> Multi<T> distinct(Multi<T> upstream) {
        return Infrastructure.onMultiCreation(new MultiDistinctOp<>(upstream));
    }

    public static <T> Multi<T> dropRepetitions(Multi<T> upstream) {
        return Infrastructure.onMultiCreation(new MultiDistinctUntilChangedOp<>(upstream));
    }

}
