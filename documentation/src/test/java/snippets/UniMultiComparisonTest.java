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

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniMultiComparisonTest {

    @Test
    public void comparison() {
        //tag::code[]
        Multi.createFrom().items("a", "b", "c")
                .onItem().transform(i -> i.toUpperCase())
                .subscribe().with(
                        item -> System.out.println("Received: " + item),
                        failure -> System.out.println("Failed with " + failure.getMessage()));

        Uni.createFrom().item("a")
                .onItem().transform(i -> i.toUpperCase())
                .subscribe().with(
                        item -> System.out.println("Received: " + item),
                        failure -> System.out.println("Failed with " + failure.getMessage()));

        //end::code[]
    }

    @Test
    public void conversion() {
        //tag::conversion[]
        Multi.createFrom().items("a", "b", "c")
                .onItem().transform(i -> i.toUpperCase())
                .toUni() // Convert the multi to uni, only "a" will be forwarded.
                .subscribe().with(
                        item -> System.out.println("Received: " + item),
                        failure -> System.out.println("Failed with " + failure.getMessage()));

        Uni.createFrom().item("a")
                .onItem().transform(i -> i.toUpperCase())
                .toMulti() // Convert the uni to a multi, the completion event will be fired after the emission of "a"
                .subscribe().with(
                        item -> System.out.println("Received: " + item),
                        failure -> System.out.println("Failed with " + failure.getMessage()));

        //end::conversion[]
    }
}
