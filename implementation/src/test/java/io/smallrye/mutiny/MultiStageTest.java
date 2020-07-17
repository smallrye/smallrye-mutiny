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
package io.smallrye.mutiny;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

public class MultiStageTest {

    @Test
    public void testChainStage() {
        List<String> result = Multi.createFrom().items(1, 2, 3)
                .stage(self -> self.onItem()
                        .transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                        .concatenate())
                .stage(self -> self
                        .onItem().transform(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .stage(m -> m.onItem().transform(i -> Integer.toString(i)))
                .stage(m -> m.collectItems().asList())
                .await().indefinitely();
        assertThat(result).containsExactly("2", "3", "4");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testChainWithDeprecatedThenAndApply() {
        List<String> result = Multi.createFrom().items(1, 2, 3)
                .then(self -> self.onItem()
                        .transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                        .concatenate())
                .then(self -> self
                        .onItem().apply(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .then(m -> m.onItem().apply(i -> Integer.toString(i)))
                .then(m -> m.collectItems().asList())
                .await().indefinitely();
        assertThat(result).containsExactly("2", "3", "4");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFunctionMustNotBeNull() {
        Multi.createFrom().item(1)
                .stage(null);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*boom.*")
    public void testThatFunctionMustNotThrowException() {
        Multi.createFrom().item(1)
                .stage(i -> {
                    throw new IllegalStateException("boom");
                });
    }

    @Test
    public void testThatFunctionCanReturnNullIfVoid() {
        AtomicReference<String> result = new AtomicReference<>();
        Void x = Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .stage(self -> self
                        .onItem().transform(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .stage(self -> self.onItem().transformToUni(i -> Uni.createFrom().item(Integer.toString(i))).concatenate())
                .stage(self -> {
                    String r = self.collectItems().first().await().indefinitely();
                    result.set(r);
                    return null; // void
                });
        assertThat(result).hasValue("24");
        assertThat(x).isNull();
    }

    @Test
    public void testChainingUni() {
        String result = Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .stage(self -> self
                        .onItem().transform(i -> i + 1)
                        .onItem().transform(i -> Integer.toString(i)))
                .stage(self -> self.collectItems().first())
                .stage(self -> self.await().indefinitely());
        assertThat(result).isEqualTo("24");
    }

    @Test
    public void testChainingUniWithDeprecatedApplyAndThen() {
        String result = Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .then(self -> self
                        .onItem().apply(i -> i + 1)
                        .onItem().apply(i -> Integer.toString(i)))
                .then(self -> self.collectItems().first())
                .then(self -> self.await().indefinitely());
        assertThat(result).isEqualTo("24");
    }

}
