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
package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.reactivestreams.Publisher;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiTransformByMergingTest {

    @Test
    public void testMerging() {
        Multi<Integer> m1 = Multi.createFrom().range(1, 10);
        Multi<Integer> m2 = Multi.createFrom().range(10, 12);

        List<Integer> list = m1.transform().byMergingWith(m2).collectItems().asList().await().indefinitely();
        assertThat(list).hasSize(11);
    }

    @Test
    public void testMergingIterable() {
        Multi<Integer> m1 = Multi.createFrom().range(1, 10);
        Multi<Integer> m2 = Multi.createFrom().range(10, 12);
        Multi<Integer> m3 = Multi.createFrom().range(12, 14);

        List<Integer> list = m1.transform().byMergingWith(Arrays.asList(m2, m3)).collectItems().asList().await()
                .indefinitely();
        assertThat(list).hasSize(13);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMergingWithNull() {
        Multi.createFrom().item(1).transform()
                .byMergingWith(Multi.createFrom().item(2), null, Multi.createFrom().item(3));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMergingWithIterableContainingNull() {
        Multi.createFrom().item(1).transform()
                .byMergingWith(Arrays.asList(Multi.createFrom().item(2), null, Multi.createFrom().item(3)));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMergingWithNullIterable() {
        Multi.createFrom().item(1).transform()
                .byMergingWith((Iterable<Publisher<Integer>>) null);
    }

    @Test
    @Ignore("this test is failing on CI - must be investigated")
    public void testConcurrentEmissionWithMerge() {
        ExecutorService service = Executors.newFixedThreadPool(10);
        Multi<Integer> m1 = Multi.createFrom().range(1, 100).emitOn(service);
        Multi<Integer> m2 = Multi.createFrom().range(100, 150).emitOn(service).emitOn(service).emitOn(service);
        Multi<Integer> m3 = Multi.createFrom().range(150, 200).emitOn(service).emitOn(service);

        Multi<Integer> merged = m1.transform().byMergingWith(m2, m3);
        MultiAssertSubscriber<Integer> subscriber = merged.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1000));

        subscriber.await();
        List<Integer> items = subscriber.items();
        assertThat(Collections.singleton(items)).noneSatisfy(list -> assertThat(list).isSorted());
    }
}
