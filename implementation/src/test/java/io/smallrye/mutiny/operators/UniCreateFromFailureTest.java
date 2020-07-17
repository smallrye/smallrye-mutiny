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
import static org.assertj.core.api.Fail.fail;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UniCreateFromFailureTest {

    @Test
    public void testWithASupplier() {
        Uni<Object> boom = Uni.createFrom().failure(() -> new IOException("boom"));
        try {
            boom.await().indefinitely();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreationWithCheckedException() {
        UniAssertSubscriber<Object> ts = UniAssertSubscriber.create();
        Uni.createFrom().failure(new Exception("boom")).subscribe().withSubscriber(ts);
        ts.assertFailure(Exception.class, "boom");

        try {
            Uni.createFrom().failure(new Exception("boom")).await().asOptional().indefinitely();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(Exception.class)
                    .isInstanceOf(RuntimeException.class);
        }

    }

    @Test
    public void testCreationWithRuntimeException() {
        UniAssertSubscriber<Object> ts = UniAssertSubscriber.create();
        Uni.createFrom().failure(new RuntimeException("boom")).subscribe().withSubscriber(ts);
        ts.assertFailure(RuntimeException.class, "boom");

        try {
            Uni.createFrom().failure(new RuntimeException("boom")).await().indefinitely();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("boom");
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreationWithNull() {
        Uni.createFrom().failure((Throwable) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreationWithNullAsSupplier() {
        Uni.createFrom().failure((Supplier<Throwable>) null);
    }

    @Test
    public void testWithASupplierReturningNull() {
        Uni<Object> boom = Uni.createFrom().failure(() -> null);
        try {
            boom.await().indefinitely();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testWithASupplierThrowingAnException() {
        Uni<Object> boom = Uni.createFrom().failure(() -> {
            throw new NoSuchElementException("boom");
        });
        try {
            boom.await().indefinitely();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NoSuchElementException.class);
        }
    }

}
