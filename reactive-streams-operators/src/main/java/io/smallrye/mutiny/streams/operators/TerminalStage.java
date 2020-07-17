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
package io.smallrye.mutiny.streams.operators;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;

/**
 * Defines a terminal stage - so a stream subscription and observation.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FunctionalInterface
public interface TerminalStage<I, O> extends Function<Multi<I>, CompletionStage<O>> {

    /**
     * Creates the {@link CompletionStage} called when the embedded logic has completed or failed.
     *
     * @param multi the observed / subscribed stream
     * @return the asynchronous result
     */
    CompletionStage<O> apply(Multi<I> multi);

}
