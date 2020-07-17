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

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UniNullTest {

    @Test
    public void uni() {
        Uni<String> uni = Uni.createFrom().item(() -> null);
        // tag::code[]
        uni.onItem().ifNull().continueWith("hello");
        uni.onItem().ifNull().switchTo(() -> Uni.createFrom().item("hello"));
        uni.onItem().ifNull().failWith(() -> new Exception("Boom!"));
        // end::code[]

        assertThat(uni.onItem().ifNull().continueWith("hello").await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void uniNotNull() {
        Uni<String> uni = Uni.createFrom().item(() -> null);
        // tag::code-not-null[]
        uni
                .onItem().ifNotNull().transform(String::toUpperCase)
                .onItem().ifNull().continueWith("yolo!");
        // end::code-not-null[]

        String r = uni
                .onItem().ifNotNull().transform(String::toUpperCase)
                .onItem().ifNull().continueWith("yolo!")
                .await().indefinitely();
        assertThat(r).isEqualTo("yolo!");
    }

    @Test
    public void accumulate() {
        Multi<Integer> multi = Multi.createFrom().range(1, 3);
        // tag::acc[]
        Multi<Integer> added = multi.onItem().scan(() -> 0, (item, acc) -> acc + item);
        // end::acc[]
        assertThat(added.subscribe().asIterable()).containsExactly(0, 1, 3);

    }
}
