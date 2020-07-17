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

import java.time.Duration;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class MergeVsConcatenateTest {

    Random random = new Random();

    public Uni<String> asyncService(String input) {
        int delay = random.nextInt(100) + 1;
        return Uni.createFrom().item(input::toUpperCase)
                .onItem().delayIt().by(Duration.ofMillis(delay));
    }

    Multi<String> multi = Multi.createFrom().items("a", "b", "c", "d", "e", "f", "g", "h");

    @Test
    public void testConcatenate() {
        // tag::concatenate[]
        List<String> list = multi
                .onItem().transformToUniAndConcatenate(this::asyncService)
                .collectItems().asList()
                .await().indefinitely();
        // end::concatenate[]
        assertThat(list).containsExactly("A", "B", "C", "D", "E", "F", "G", "H");
    }

    @Test
    public void testMerge() {
        // tag::merge[]
        List<String> list = multi
                .onItem().transformToUniAndMerge(this::asyncService)
                .collectItems().asList()
                .await().indefinitely();
        // end::merge[]
        assertThat(list).containsExactlyInAnyOrder("A", "B", "C", "D", "E", "F", "G", "H");
    }

    @Test
    public void testMergeWithConcurrency() {
        // tag::merge-concurrency[]
        List<String> list = multi
                .onItem().transformToUni(this::asyncService).merge(8)
                .collectItems().asList()
                .await().indefinitely();
        // end::merge-concurrency[]
        assertThat(list).containsExactlyInAnyOrder("A", "B", "C", "D", "E", "F", "G", "H");
    }

}
