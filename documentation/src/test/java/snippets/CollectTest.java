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
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.BlockingIterable;

public class CollectTest {

    @Test
    public void collect() {
        Multi<Integer> multi = Multi.createFrom().range(1, 3);
        // tag::code[]

        // collectItems let you collect items into a Uni of data structure.
        // The final result is emitted when the completion event is received
        Uni<List<Integer>> list = multi.collectItems().asList();
        Uni<Map<String, Integer>> map = multi.collectItems().asMap(i -> Integer.toString(i));

        // You can retrieve the first and last items
        Uni<Integer> first = multi.collectItems().first();
        Uni<Integer> last = multi.collectItems().last();

        // you can also get a **blocking** iterable / streams
        BlockingIterable<Integer> integers = multi.subscribe().asIterable();
        Stream<Integer> stream = multi.subscribe().asStream();

        // end::code[]

        assertThat(list.await().indefinitely()).containsExactly(1, 2);
        assertThat(map.await().indefinitely()).hasSize(2).containsKeys("1", "2");
        assertThat(first.await().indefinitely()).isEqualTo(1);
        assertThat(last.await().indefinitely()).isEqualTo(2);
        assertThat(integers).containsExactly(1, 2);
        assertThat(stream).containsExactly(1, 2);
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
