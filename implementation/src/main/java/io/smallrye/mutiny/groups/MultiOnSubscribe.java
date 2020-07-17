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

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOnSubscribeInvokeOp;
import io.smallrye.mutiny.operators.multi.MultiOnSubscribeInvokeUniOp;

/**
 * Group to configure the action to execute when the observed {@link Multi} sends a {@link Subscription}.
 * The downstream don't have a subscription yet. It will be passed once the configured action completes.
 *
 * For example:
 *
 * <pre>
 * {@code
 * multi.onSubscribe().invoke(sub -> System.out.println("subscribed"));
 * // Delay the subscription by 1 second (or until an asynchronous action completes)
 * multi.onSubscribe().invokeUni(sub -> Uni.createFrom(1).onItem().delayIt().by(Duration.ofSecond(1)));
 * }
 * </pre>
 *
 * @param <T> the type of item
 */
public class MultiOnSubscribe<T> {

    private final Multi<T> upstream;

    public MultiOnSubscribe(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when the {@code subscription} is received.
     * <p>
     * The callback in invoked before passing a subscription event downstream.
     * If the callback throws an exception, the downstream receives a subscription and the failure immediately.
     *
     * @param callback the callback, must not be {@code null}.
     * @return the new {@link Multi}
     */
    public Multi<T> invoke(Consumer<? super Subscription> callback) {
        return Infrastructure.onMultiCreation(
                new MultiOnSubscribeInvokeOp<>(upstream, nonNull(callback, "callback")));
    }

    /**
     * Produces a new {@link Multi} invoking the given @{code action} when the {@code subscription} event is received.
     * <p>
     * Unlike {@link #invoke(Consumer)}, the passed function returns a {@link Uni}. When the produced {@code Uni} sends
     * the subscription, the function is called. The subscription event is passed downstream only when the {@link Uni}
     * completes. If the produced {@code Uni} fails or if the function throws an exception, the failure is propagated
     * downstream.
     *
     * @param action the callback, must not be {@code null}
     * @return the new {@link Multi}
     */
    public Multi<T> invokeUni(Function<? super Subscription, Uni<?>> action) {
        return Infrastructure.onMultiCreation(
                new MultiOnSubscribeInvokeUniOp<>(upstream, nonNull(action, "action")));
    }

}
