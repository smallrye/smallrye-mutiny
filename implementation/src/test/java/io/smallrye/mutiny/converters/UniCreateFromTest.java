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
package io.smallrye.mutiny.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.BuiltinConverters;

public class UniCreateFromTest {

    @Test
    public void testCreatingFromCompletionStages() {
        CompletableFuture<Integer> valued = CompletableFuture.completedFuture(1);
        CompletableFuture<Void> empty = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> boom = new CompletableFuture<>();
        boom.completeExceptionally(new Exception("boom"));

        Uni<Integer> u1 = Uni.createFrom().completionStage(valued);
        Uni<Void> u2 = Uni.createFrom().completionStage(empty);
        Uni<Void> u3 = Uni.createFrom().converter(BuiltinConverters.fromCompletionStage(), boom);

        assertThat(u1.await().asOptional().indefinitely()).contains(1);
        assertThat(u2.await().indefinitely()).isEqualTo(null);
        try {
            u3.await();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(Exception.class);
        }
    }

}
