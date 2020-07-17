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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UniOnNullSwitchToTest {

    private Uni<Integer> fallback = Uni.createFrom().item(23);
    private Uni<Integer> failure = Uni.createFrom().failure(new IOException("boom"));

    @Test
    public void testSwitchToFallback() {
        assertThat(Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo(fallback)
                .await().indefinitely()).isEqualTo(23);
    }

    @Test
    public void testSwitchToSupplierFallback() {
        AtomicInteger count = new AtomicInteger();
        assertThat(Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo(() -> fallback.map(i -> i + count.incrementAndGet()))
                .await().indefinitely()).isEqualTo(24);

        assertThat(Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo(() -> fallback.map(i -> i + count.incrementAndGet()))
                .await().indefinitely()).isEqualTo(25);
    }

    @Test
    public void testSwitchToFailure() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                        .onItem().ifNull().switchTo(failure)
                        .await().indefinitely())
                .withMessageEndingWith("boom");

    }

    @Test
    public void testSwitchToSupplierFailure() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                        .onItem().ifNull().switchTo(() -> failure)
                        .await().indefinitely())
                .withMessageEndingWith("boom");

    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testSwitchToNull() {
        Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo((Uni<Integer>) null)
                .await().indefinitely();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testSwitchToNullSupplier() {
        Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo((Uni<? extends Integer>) null)
                .await().indefinitely();
    }

}
