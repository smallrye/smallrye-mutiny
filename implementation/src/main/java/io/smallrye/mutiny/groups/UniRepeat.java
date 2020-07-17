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
package io.smallrye.mutiny.groups;

import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.multi.MultiRepeatUntilOp;
import io.smallrye.mutiny.operators.multi.MultiRepeatWhilstOp;

/**
 * Repeatedly subscribes to a given {@link Uni} to generate a {@link Multi}.
 *
 * @param <T> the type of item
 */
public class UniRepeat<T> {

    private final Uni<T> upstream;

    public UniRepeat(Uni<T> upstream) {
        this.upstream = upstream;
    }

    /**
     * Generates an unbounded stream, indefinitely resubscribing to the {@link Uni}.
     * Note that this enforces:
     * <ul>
     * <li>the number of requests coming from the subscriber</li>
     * <li>cancellation</li>
     * <li>failures, that are propagated downstream</li>
     * </ul>
     * <p>
     * The produced {@link Multi} contains the items emitted by the upstream {@link Uni}. After every emission,
     * another subscription is performed on the {@link Uni}, and the item is then propagated. If the {@link Uni}
     * fires a failure, the failure is propagated. If the {@link Uni} fires an empty items, it resubscribes.
     *
     * @return the {@link Multi} containing the items from the upstream {@link Uni}, resubscribed indefinitely.
     */
    public Multi<T> indefinitely() {
        return atMost(Long.MAX_VALUE);
    }

    /**
     * Generates a stream, containing the items from the upstream {@link Uni}, resubscribed at most {@code times} times.
     * <p>
     * Note that this enforces:
     * <ul>
     * <li>the number of requests coming from the subscriber</li>
     * <li>cancellation</li>
     * <li>failures, that are propagated downstream</li>
     * </ul>
     * <p>
     * The produced {@link Multi} contains the items emitted by the upstream {@link Uni}. After every emission,
     * another subscription is performed on the {@link Uni}, and the item is then propagated. If the {@link Uni}
     * fires a failure, the failure is propagated. If the {@link Uni} fires an empty items, it resubscribes.
     * <p>
     * This method is named {@code atMost} because the repeating re-subscription can be stopped if the subscriber
     * cancels its subscription to the produced {@link Multi}.
     *
     * @param times the number of re-subscription, must be strictly positive, 1 is equivalent to {@link Uni#toMulti()}
     * @return the {@link Multi} containing the items from the upstream {@link Uni}, resubscribed at most {@code times}
     *         times.
     */
    public Multi<T> atMost(long times) {
        long actual = ParameterValidation.positive(times, "times");
        return new MultiRepeatUntilOp<>(upstream.toMulti(), actual);
    }

    /**
     * Generates a stream, containing the items from the upstream {@link Uni}, resubscribed until the given predicate
     * returns {@code true}. The predicate is called on the item produced by the {@link Uni}. If it does not pass, the
     * item is not propagated downstream and the repetition is stopped.
     *
     * Unlike {@link #whilst(Predicate)}, the checked item is only propagated downstream if it passed the predicate.
     * For example, if you use an API returning "null" or an empty set once you reach the end, you can stop the
     * repetition when this case is detected.
     *
     * The predicate is not called on {@code null} item. If you want to intercept this case, use a sentinel item.
     *
     * If the Uni propagates a failure, the failure is propagated and the repetition stopped.
     *
     * @param predicate the predicate, must not be {@code null}
     * @return the {@link Multi} containing the items from the upstream {@link Uni}, resubscribed until the predicate
     *         returns {@code true}.
     */
    public Multi<T> until(Predicate<T> predicate) {
        return new MultiRepeatUntilOp<>(upstream.toMulti(), ParameterValidation.nonNull(predicate, "predicate"));
    }

    /**
     * Generates a stream, containing the items from the upstream {@link Uni}, resubscribed while the given predicate
     * returns {@code true}.
     *
     * The uni is subscribed at least once. The item is checked. Regardless the result of the predicate, the item
     * is propagated downstream. If the test passed, the repetition continues, otherwise the repetition is stopped.
     *
     * Unlike {@link #until(Predicate)}, the checked item is propagated downstream regardless if it passed the predicate.
     * For example, if you use a Rest API specifying the "next page", you can stop the repetition when the "next page"
     * is absent, while still propagating downstream the current page.
     *
     * The predicate is not called on {@code null} item. If you want to intercept this case, use a sentinel item.
     *
     * If the Uni propagates a failure, the failure is propagated and the repetition stopped.
     *
     * @param predicate the predicate, must not be {@code null}
     * @return the {@link Multi} containing the items from the upstream {@link Uni}, resubscribed until the predicate
     *         returns {@code true}.
     */
    public Multi<T> whilst(Predicate<T> predicate) {
        return new MultiRepeatWhilstOp<>(upstream.toMulti(), ParameterValidation.nonNull(predicate, "predicate"));
    }

}
