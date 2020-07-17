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

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOnTerminationInvoke;
import io.smallrye.mutiny.operators.multi.MultiOnTerminationInvokeUni;

public class MultiOnTerminate<T> {

    private final Multi<T> upstream;

    public MultiOnTerminate(Multi<T> upstream) {
        this.upstream = ParameterValidation.nonNull(upstream, "upstream");
    }

    /**
     * Attaches an action that is executed when the {@link Multi} emits a completion or a failure or when the subscriber
     * cancels the subscription.
     *
     * @param callback the consumer receiving the failure if any and a boolean indicating whether the termination
     *        is due to a cancellation (the failure parameter would be {@code null} in this case). Must not
     *        be {@code null}.
     * @return the new {@link Multi}
     */
    public Multi<T> invoke(BiConsumer<Throwable, Boolean> callback) {
        return Infrastructure.onMultiCreation(new MultiOnTerminationInvoke<>(upstream, nonNull(callback, "callback")));
    }

    /**
     * Attaches an action that is executed when the {@link Multi} emits a completion or a failure or when the subscriber
     * cancels the subscription. Unlike {@link #invoke(BiConsumer)}, the callback does not receive the failure or
     * cancellation details.
     *
     * @param action the action to execute when the streams completes, fails or the subscription gets cancelled. Must
     *        not be {@code null}.
     * @return the new {@link Multi}
     */
    public Multi<T> invoke(Runnable action) {
        Runnable runnable = nonNull(action, "action");
        return Infrastructure.onMultiCreation(new MultiOnTerminationInvoke<>(upstream, (f, c) -> runnable.run()));
    }

    /**
     * Attaches an action that is executed when the {@link Multi} mits a completion or a failure or when the subscriber
     * cancels the subscription.
     * 
     * @param mapper the function to execute where the first argument is a non-{@code null} exception on failure, and
     *        the second argument is a boolean which is {@code true} when the subscriber cancels the subscription.
     *        The function returns a {@link Uni}.
     * @return the new {@link Multi}
     */
    public Multi<T> invokeUni(BiFunction<Throwable, Boolean, Uni<?>> mapper) {
        return Infrastructure.onMultiCreation(new MultiOnTerminationInvokeUni<>(upstream, mapper));
    }
}
