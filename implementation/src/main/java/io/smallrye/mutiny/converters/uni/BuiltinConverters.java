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
package io.smallrye.mutiny.converters.uni;

public class BuiltinConverters {
    private BuiltinConverters() {
        // Avoid direct instantiation
    }

    @SuppressWarnings("unchecked")
    public static <T> FromCompletionStage<T> fromCompletionStage() {
        return FromCompletionStage.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToCompletionStage<T> toCompletionStage() {
        return ToCompletionStage.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToCompletableFuture<T> toCompletableFuture() {
        return ToCompletableFuture.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToPublisher<T> toPublisher() {
        return ToPublisher.INSTANCE;
    }
}
