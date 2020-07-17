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
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

public class UniAwaitTest {

    @Test(timeOut = 1000)
    public void testAwaitingOnAnAlreadyResolvedUni() {
        assertThat(Uni.createFrom().item(1).await().indefinitely()).isEqualTo(1);
    }

    @Test(timeOut = 100)
    public void testAwaitingOnAnAlreadyResolvedWitNullUni() {
        assertThat(Uni.createFrom().item((Object) null).await().indefinitely()).isNull();
    }

    @Test(timeOut = 100)
    public void testAwaitingOnAnAlreadyFailedUni() {
        try {
            Uni.createFrom().failure(new IOException("boom")).await().indefinitely();
            fail("Exception expected");
        } catch (CompletionException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class).hasMessageEndingWith("boom");
        }
    }

    // Uni.createFrom().failure before onTimeout

    @Test(timeOut = 1000)
    public void testAwaitingOnAnAsyncUni() {
        assertThat(
                Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.complete(1)).start()).await()
                        .indefinitely()).isEqualTo(1);
    }

    @Test(timeOut = 1000)
    public void testAwaitingOnAnAsyncFailingUni() {
        try {
            Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.fail(new IOException("boom"))).start()).await()
                    .indefinitely();
            fail("Exception expected");
        } catch (CompletionException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class).hasMessageEndingWith("boom");
        }
    }

    @Test(timeOut = 100)
    public void testAwaitWithTimeOut() {
        assertThat(Uni.createFrom().item(1).await().atMost(Duration.ofMillis(1000))).isEqualTo(1);
    }

    @Test(timeOut = 100, expectedExceptions = TimeoutException.class)
    public void testTimeout() {
        Uni.createFrom().nothing().await().atMost(Duration.ofMillis(10));
    }

    @Test(timeOut = 5000)
    public void testInterruptedTimeout() {
        AtomicBoolean awaiting = new AtomicBoolean();
        AtomicReference<RuntimeException> exception = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                awaiting.set(true);
                Uni.createFrom().nothing().await().atMost(Duration.ofMillis(1000));
            } catch (RuntimeException e) {
                exception.set(e);
            }
        });
        thread.start();
        await().untilTrue(awaiting);
        thread.interrupt();
        await().until(() -> exception.get() != null);
        assertThat(exception.get()).hasCauseInstanceOf(InterruptedException.class);
    }

    @Test
    public void testAwaitAsOptionalWithResult() {
        assertThat(
                Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.complete(1)).start()).await().asOptional()
                        .indefinitely()).contains(1);
    }

    @Test
    public void testAwaitAsOptionalWithFailure() {
        try {
            Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.fail(new IOException("boom"))).start())
                    .await().asOptional().indefinitely();
            fail("Exception expected");
        } catch (CompletionException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class).hasMessageEndingWith("boom");
        }
    }

    @Test
    public void testAwaitAsOptionalWithNull() {
        assertThat(
                Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.complete(null)).start()).await()
                        .asOptional().indefinitely()).isEmpty();
    }

    @Test
    public void testAwaitAsOptionalWithTimeout() {
        assertThat(
                Uni.createFrom().emitter(emitter -> new Thread(() -> emitter.complete(1)).start()).await().asOptional()
                        .atMost(Duration.ofMillis(1000))).contains(1);
    }

    @Test(timeOut = 100, expectedExceptions = TimeoutException.class)
    public void testTimeoutAndOptional() {
        Uni.createFrom().nothing().await().asOptional().atMost(Duration.ofMillis(10));
    }

}
