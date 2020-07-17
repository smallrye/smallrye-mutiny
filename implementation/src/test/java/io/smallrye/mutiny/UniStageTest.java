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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

public class UniStageTest {

    @Test
    public void testChainStage() {
        String result = Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .stage(self -> self
                        .onItem().transform(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .stage(self -> self.onItem().transformToUni(i -> Uni.createFrom().item(Integer.toString(i))))
                .await().indefinitely();
        assertThat(result).isEqualTo("24");
    }

    @Test
    public void testChainThenWithDeprecatedApplyAndThen() {
        String result = Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .then(self -> self
                        .onItem().apply(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .then(self -> self.onItem().transformToUni(i -> Uni.createFrom().item(Integer.toString(i))))
                .await().indefinitely();
        assertThat(result).isEqualTo("24");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFunctionMustNotBeNull() {
        Uni.createFrom().item(1)
                .stage(null);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*boom.*")
    public void testThatFunctionMustNotThrowException() {
        Uni.createFrom().item(1)
                .stage(i -> {
                    throw new IllegalStateException("boom");
                });
    }

    @Test
    public void testThatFunctionCanReturnNullIfVoid() {
        AtomicReference<String> result = new AtomicReference<>();
        Void x = Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .stage(self -> self
                        .onItem().transform(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .stage(self -> self.onItem().transformToUni(i -> Uni.createFrom().item(Integer.toString(i))))
                .stage(self -> {
                    String r = self.await().indefinitely();
                    result.set(r);
                    return null; // void
                });
        assertThat(result).hasValue("24");
        assertThat(x).isNull();
    }

    @Test
    public void testChainingMulti() {
        String result = Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .stage(self -> Multi.createFrom().uni(self))
                .stage(self -> self
                        .onItem().transform(i -> i + 1)
                        .onItem().transform(i -> Integer.toString(i)))
                .stage(self -> self.collectItems().first())
                .stage(self -> self.await().indefinitely());
        assertThat(result).isEqualTo("24");
    }

}
