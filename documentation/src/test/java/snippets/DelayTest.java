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

public class DelayTest {

    @Test
    public void testDelayBy() {
        // tag::delay-by[]
        String delayed = Uni.createFrom().item("hello")
                .onItem().delayIt().by(Duration.ofMillis(10))
                .map(s -> "Delayed " + s)
                .await().indefinitely();
        // end::delay-by[]
        assertThat(delayed).isEqualTo("Delayed hello");
    }

    @Test
    public void testDelayUntil() {
        // tag::delay-until[]
        String delayed = Uni.createFrom().item("hello")
                // The write method returns a Uni completed
                // when the operation is done.
                .onItem().delayIt().until(this::write)
                .map(s -> "Written " + s)
                .await().indefinitely();
        // end::delay-until[]
        assertThat(delayed).isEqualTo("Written hello");
    }

    @Test
    public void testDelayMulti() {
        // tag::delay-multi[]
        List<Integer> delayed = Multi.createFrom().items(1, 2, 3, 4, 5)
                .onItem().transformToUni(i -> Uni.createFrom().item(i).onItem().delayIt().by(Duration.ofMillis(10)))
                .concatenate()
                .collectItems().asList()
                .await().indefinitely();
        // end::delay-multi[]
        assertThat(delayed).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    public void testDelayMultiRandom() {
        // tag::delay-multi-random[]
        Random random = new Random();
        List<Integer> delayed = Multi.createFrom().items(1, 2, 3, 4, 5)
                .onItem().transformToUni(i -> Uni.createFrom().item(i).onItem().delayIt().by(Duration.ofMillis(random.nextInt(100) + 1)))
                .merge()
                .collectItems().asList()
                .await().indefinitely();
        // end::delay-multi-random[]
        assertThat(delayed).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
    }

    private Uni<Void> write(String s) {
        return Uni.createFrom().item(s)
                .onItem().delayIt().by(Duration.ofMillis(20))
                .onItem().ignore().andContinueWithNull();
    }

}
