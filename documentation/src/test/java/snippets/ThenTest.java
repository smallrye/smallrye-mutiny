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

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.smallrye.mutiny.Multi;

public class ThenTest {

    @Test
    public void test() {
        // tag::code[]
        String result = Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .stage(self -> {
                    // Transform each item into a string of the item +1
                    return self
                            .onItem().transform(i -> i + 1)
                            .onItem().transform(i -> Integer.toString(i));
                })
                .stage(self -> self
                        .onItem().invoke(item -> System.out.println("The item is " + item))
                        .collectItems().first())
                .stage(self -> self.await().indefinitely());
        // end::code[]
        assertThat(result).isEqualTo("24");
    }
}
