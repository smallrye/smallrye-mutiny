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
import java.time.Duration;

import org.testng.annotations.Test;

import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

public class UniIfNoItemTest {

    @Test
    public void testResultWhenTimeoutIsNotReached() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        Uni.createFrom().item(1)
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithUni(Uni.createFrom().nothing())
                .subscribe().withSubscriber(ts);

        ts.await().assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testTimeout() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        Uni.createFrom().item(1)
                .onItem().delayIt().by(Duration.ofMillis(10))
                .ifNoItem().after(Duration.ofMillis(1)).fail()
                .subscribe().withSubscriber(ts);

        ts.await().assertCompletedWithFailure();
        assertThat(ts.getFailure()).isInstanceOf(TimeoutException.class);

    }

    @Test
    public void testRecoverWithItem() {
        UniAssertSubscriber<Integer> ts = Uni.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithItem(5)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        ts.await().assertItem(5);
    }

    @Test
    public void testRecoverWithItemSupplier() {
        UniAssertSubscriber<Integer> ts = Uni.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithItem(() -> 23)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        ts.await().assertItem(23);
    }

    @Test
    public void testRecoverWithSwitchToUni() {
        UniAssertSubscriber<Integer> ts = Uni.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithUni(() -> Uni.createFrom().item(15))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        ts.await().assertItem(15);
    }

    @Test
    public void testFailingWithAnotherException() {
        UniAssertSubscriber<Integer> ts = Uni.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).failWith(new IOException("boom"))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        ts.await().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testDurationValidity() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Uni.createFrom().item(1).ifNoItem().after(null))
                .withMessageContaining("timeout");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Uni.createFrom().item(1).ifNoItem().after(Duration.ofMillis(0)))
                .withMessageContaining("timeout");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Uni.createFrom().item(1).ifNoItem().after(Duration.ofMillis(-1)))
                .withMessageContaining("timeout");
    }

}
