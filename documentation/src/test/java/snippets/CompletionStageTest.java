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
package snippets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class CompletionStageTest {

    @Test
    public void test() {
        // tag::code[]
        CompletableFuture<String> future1 = Uni
                // Create from a Completion Stage
                .createFrom().completionStage(CompletableFuture.supplyAsync(() -> "hello"))
                .map(String::toUpperCase)
                .subscribeAsCompletionStage(); // Retrieve as a Completion Stage

        CompletableFuture<List<String>> future2 = Multi
                .createFrom().completionStage(CompletableFuture.supplyAsync(() -> "hello"))
                .map(String::toUpperCase)
                .collectItems().asList() // Accumulate items in a list (return a Uni<List<T>>)
                .subscribeAsCompletionStage();// Retrieve the list as a Completion Stage

        // end::code[]
        assertThat(future1.join()).isEqualTo("HELLO");
        assertThat(future2.join()).containsExactly("HELLO");
    }
}
