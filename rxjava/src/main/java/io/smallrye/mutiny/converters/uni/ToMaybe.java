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

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.reactivex.Maybe;
import io.smallrye.mutiny.Uni;

public class ToMaybe<T> implements Function<Uni<T>, Maybe<T>> {
    public static final ToMaybe INSTANCE = new ToMaybe();

    private ToMaybe() {
        // Avoid direct instantiation
    }

    @Override
    public Maybe<T> apply(Uni<T> uni) {
        return Maybe.create(emitter -> {
            CompletableFuture<T> future = uni.subscribe().asCompletionStage();
            emitter.setCancellable(() -> future.cancel(false));
            future.whenComplete((res, fail) -> {
                if (future.isCancelled()) {
                    return;
                }

                if (fail != null) {
                    emitter.onError(fail);
                } else if (res != null) {
                    emitter.onSuccess(res);
                    emitter.onComplete();
                } else {
                    emitter.onComplete();
                }

            });
        });
    }
}
