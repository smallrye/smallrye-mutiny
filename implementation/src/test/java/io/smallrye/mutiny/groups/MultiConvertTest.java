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
package io.smallrye.mutiny.groups;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiConvertTest {

    @Test
    public void testMultiConvertWithCustomConverter() {
        Multi<String> multi = Multi.createFrom().items(1, 2, 3).convert().with(m -> m.map(i -> Integer.toString(i)));
        List<String> list = multi.collectItems().asList().await().indefinitely();
        assertThat(list).containsExactly("1", "2", "3");
    }

    @Test
    public void testMultiConvertToPublisher() {
        Multi<Integer> items = Multi.createFrom().items(1, 2, 3);
        Publisher<Integer> publisher = items.convert().toPublisher();
        assertThat(items).isSameAs(publisher);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatConverterCannotBeNull() {
        Multi.createFrom().items(1, 2, 3).convert().with(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatUpstreamCannotBeNull() {
        new MultiConvert<>(null);
    }

}
