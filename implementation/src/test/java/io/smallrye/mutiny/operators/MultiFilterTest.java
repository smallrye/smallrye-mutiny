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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MultiFilterTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatPredicateCannotBeNull() {
        Multi.createFrom().range(1, 4)
                .transform().byFilteringItemsWith(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFunctionCannotBeNull() {
        Multi.createFrom().range(1, 4)
                .transform().byTestingItemsWith(null);
    }

    @Test
    public void testFilteringWithPredicate() {
        Predicate<Integer> test = x -> x % 2 != 0;
        assertThat(Multi.createFrom().range(1, 4)
                .transform().byFilteringItemsWith(test)
                .collectItems().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }

    @Test
    public void testFilteringWithUni() {
        assertThat(Multi.createFrom().range(1, 4)
                .transform()
                .byTestingItemsWith(
                        x -> Uni.createFrom().completionStage(() -> CompletableFuture.supplyAsync(() -> x % 2 != 0)))
                .collectItems().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }

    @Test
    public void testFilterShortcut() {
        Predicate<Integer> test = x -> x % 2 != 0;
        assertThat(Multi.createFrom().range(1, 4)
                .filter(test)
                .collectItems().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }
}
