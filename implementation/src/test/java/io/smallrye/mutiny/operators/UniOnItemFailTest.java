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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UniOnItemFailTest {

    private Uni<Integer> one = Uni.createFrom().item(1);

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatMapperCannotBeNull() {
        one.onItem().failWith(null);
    }

    @Test
    public void testMapToException() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = one.onItem().failWith(s -> new IOException(Integer.toString(s + count.getAndIncrement())));
        uni
                .subscribe().withSubscriber(UniAssertSubscriber.<Number> create())
                .assertFailure(IOException.class, "1");
        uni
                .subscribe().withSubscriber(UniAssertSubscriber.<Number> create())
                .assertFailure(IOException.class, "2");
    }

}
