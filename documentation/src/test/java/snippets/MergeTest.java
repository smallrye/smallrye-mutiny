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

import org.junit.Test;

import io.smallrye.mutiny.Multi;

public class MergeTest {

    @Test
    public void merge() {
        Multi<Integer> multi1 = Multi.createFrom().range(1, 3);
        Multi<Integer> multi2 = Multi.createFrom().range(3, 6);
        Multi<Integer> multi3 = Multi.createFrom().range(6, 9);
        // tag::code[]

        List<Integer> list1 = Multi.createBy().merging().streams(multi1, multi2, multi3)
                .collectItems().asList()
                .await().indefinitely();

        List<Integer> list2 = Multi.createBy().concatenating().streams(multi1, multi2, multi3)
                .collectItems().asList()
                .await().indefinitely();

        List<Integer> list3 = Multi.createBy()
                .combining().streams(multi1, multi2, multi3).using((a, b, c) -> a + b + c)
                .collectItems().asList()
                .await().indefinitely();
        // end::code[]

        assertThat(list1).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(list2).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(list3).containsExactly(10, 13);
    }

}
