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
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UniOnNullFailTest {

    @Test(expectedExceptions = NoSuchElementException.class)
    public void testFail() {
        Uni.createFrom().item((Object) null)
                .onItem().ifNull().fail().await().indefinitely();
    }

    @Test
    public void testFailNotCalledOnItem() {
        assertThat(Uni.createFrom().item(1).onItem().ifNull().fail().await().indefinitely()).isEqualTo(1);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testFailWithException() {
        Uni.createFrom().item((Object) null).onItem().ifNull().failWith(new RuntimeException("boom")).await().indefinitely();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFailWithExceptionSetToNull() {
        Uni.createFrom().item((Object) null).onItem().ifNull().failWith((Exception) null).await().indefinitely();
    }

    @Test
    public void testFailWithExceptionNotCalledOnITem() {
        assertThat(Uni.createFrom().item(1).onItem().ifNull().failWith(new IOException("boom")).await().indefinitely())
                .isEqualTo(1);
    }

    @Test
    public void testFailWithExceptionSupplier() {
        AtomicInteger count = new AtomicInteger();
        Uni<Void> boom = Uni.createFrom().item((Object) null)
                .onItem().castTo(Void.class)
                .onItem().ifNull().failWith(() -> new RuntimeException(Integer.toString(count.incrementAndGet())));

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> boom.await().indefinitely())
                .withMessageEndingWith("1");
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> boom.await().indefinitely())
                .withMessageEndingWith("2");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFailWithExceptionSupplierSetToNull() {
        Uni.createFrom().item((Object) null).onItem().ifNull().failWith((Supplier<Throwable>) null).await().indefinitely();
    }

    @Test
    public void testFailWithExceptionSupplierNotCalledOnItem() {
        assertThat(Uni.createFrom().item(1).onItem().ifNull().failWith(new IOException("boom")).await().indefinitely())
                .isEqualTo(1);
    }

}
