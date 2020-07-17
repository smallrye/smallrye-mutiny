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
package io.smallrye.mutiny.streams.utils;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Processor;

import io.smallrye.mutiny.streams.operators.ProcessingStage;

/**
 * Cosmetic cast / generic / erasure fixes.
 * <p>
 * At some point this class should disappear...
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@SuppressWarnings("unchecked")
public class Casts {

    private Casts() {
        // Avoid direct instantiation.
    }

    public static <I, O> Function<I, O> cast(Function<?, ?> fun) {
        return (Function<I, O>) fun;
    }

    public static <I> Predicate<I> cast(Predicate<?> p) {
        return (Predicate<I>) p;
    }

    public static <I, O> ProcessingStage<I, O> cast(ProcessingStage<?, ?> p) {
        return (ProcessingStage<I, O>) p;
    }

    public static <I, O> Processor<I, O> cast(Processor<?, ?> p) {
        return (Processor<I, O>) p;
    }

    public static <O> CompletionStage<O> cast(CompletionStage<?> cs) {
        return (CompletionStage<O>) cs;
    }

}
