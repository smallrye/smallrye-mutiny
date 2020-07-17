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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple3;
import org.junit.Test;

import io.smallrye.mutiny.Uni;

public class HowToJoinAsyncTest {

    @Test
    public void test() {

        // tag::code[]
        Uni<String> uni1 = getFirstUni();
        Uni<String> uni2 = getSecondUni();
        Uni<Integer> uni3 = getThirdUni();

        Uni<Tuple3<String, String, Integer>> uni = Uni.combine().all().unis(uni1, uni2, uni3).asTuple();
        String result  = uni.onItem().apply(tuple ->
                tuple.getItem1() + " " + tuple.getItem2() + " " + tuple.getItem3() + " !")
                .await().indefinitely();

        // end::code[]
        assertThat(result).isEqualTo("hello world 42 !");
    }

    @Test
    public void testCombinator() {

        // tag::codeCombinator[]
        Uni<String> uni1 = getFirstUni();
        Uni<String> uni2 = getSecondUni();
        Uni<Integer> uni3 = getThirdUni();

        List<Uni<?>> list = Arrays.asList(uni1, uni2, uni3);

        Uni<String> uni = Uni.combine().all().unis(list).combinedWith(results ->
                results.get(0) + " " + results.get(1) + " " + results.get(2) + " !"
        );

        // end::codeCombinator[]
        assertThat(uni.await().indefinitely()).isEqualTo("hello world 42 !");
    }


    private Uni<String> getFirstUni() {
        return Uni.createFrom().item("hello")
                .emitOn(Infrastructure.getDefaultWorkerPool());
    }

    private Uni<String> getSecondUni() {
        return Uni.createFrom().item("world")
                .emitOn(Infrastructure.getDefaultWorkerPool());
    }

    private Uni<Integer> getThirdUni() {
        return Uni.createFrom().item(42)
                .emitOn(Infrastructure.getDefaultWorkerPool());
    }
}
