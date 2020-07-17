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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import io.smallrye.mutiny.Multi;

public class BackPressureTest {

    @Test
    public void test() {
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");

        ExecutorService executor = Executors.newFixedThreadPool(4);
        // tag::code[]

        String res1 = multi
                .emitOn(executor)
                .onOverflow().buffer(10)
                .collectItems().first()
                .await().indefinitely();

        String res2 = multi
                .emitOn(executor)
                .onOverflow().dropPreviousItems()
                .collectItems().first()
                .await().indefinitely();

        // end::code[]
        assertThat(res1).isEqualTo("a");
        assertThat(res2).isEqualTo("a");
    }
}
