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
import io.smallrye.mutiny.Uni;

public class HowToFilterTest {

    @Test
    public void filter() {
        Multi<Integer> multi = Multi.createFrom().range(1, 11);
        // tag::filter[]
        List<Integer> list = multi
                .transform().byFilteringItemsWith(i -> i > 6)
                .collectItems().asList()
                .await().indefinitely();

        List<Integer> list2 = multi
                .transform().byTestingItemsWith(i -> Uni.createFrom().item(i > 6))
                .collectItems().asList()
                .await().indefinitely();
        // end::filter[]
        assertThat(list).containsExactly(7, 8, 9, 10);
        assertThat(list2).containsExactly(7, 8, 9, 10);
    }

    @Test
    public void take() {
        Multi<Integer> multi = Multi.createFrom().range(1, 11);
        // tag::take[]
        List<Integer> list = multi
                .transform().byTakingFirstItems(2)
                .collectItems().asList()
                .await().indefinitely();

        List<Integer> list2 = multi
                .transform().byTakingItemsWhile(i -> i < 3)
                .collectItems().asList()
                .await().indefinitely();

        List<Integer> list3 = multi
                .transform().byTakingLastItems(2)
                .collectItems().asList()
                .await().indefinitely();
        // end::take[]
        assertThat(list).containsExactly(1, 2);
        assertThat(list2).containsExactly(1, 2);
        assertThat(list3).containsExactly(9, 10);
    }

    @Test
    public void skip() {
        Multi<Integer> multi = Multi.createFrom().range(1, 11);
        // tag::skip[]
        List<Integer> list = multi
                .transform().bySkippingFirstItems(8)
                .collectItems().asList()
                .await().indefinitely();

        List<Integer> list2 = multi
                .transform().bySkippingItemsWhile(i -> i < 9)
                .collectItems().asList()
                .await().indefinitely();

        List<Integer> list3 = multi
                .transform().bySkippingLastItems(8)
                .collectItems().asList()
                .await().indefinitely();
        // end::skip[]
        assertThat(list).containsExactly(9, 10);
        assertThat(list2).containsExactly(9, 10);
        assertThat(list3).containsExactly(1, 2);
    }

    @Test
    public void distinct() {
        Multi<Integer> multi = Multi.createFrom().items(1, 1, 2, 3, 4, 5, 5, 6);
        // tag::distinct[]
        List<Integer> list = multi
                .transform().byDroppingDuplicates()
                .collectItems().asList()
                .await().indefinitely();
        // end::distinct[]

        // tag::repetition[]
        List<Integer> list2 = multi
                .transform().byDroppingRepetitions()
                .collectItems().asList()
                .await().indefinitely();
        // end::repetition[]
        assertThat(list).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(list2).containsExactly(1, 2, 3, 4, 5, 6);
    }
}
