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
package io.smallrye.mutiny.operators.uni.builders;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniAssertSubscriber;

public class KnownFailureUniTest {

    @Test
    public void testCreationWithFailure() {
        assertThatThrownBy(() -> Uni.createFrom().failure(new IOException("io")).await().indefinitely())
                .hasCauseInstanceOf(IOException.class).hasMessageContaining("io");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreationWithNull() {
        Uni.createFrom().failure((Throwable) null);
    }

    @Test
    public void testCancellationAfterEmission() {
        UniAssertSubscriber<String> hello = Uni.createFrom().<String> failure(new IOException("boom"))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        hello.cancel();
        hello.assertFailure(IOException.class, "boom");
    }

    @Test
    public void testCancellationBeforeEmission() {
        UniAssertSubscriber<String> subscriber = new UniAssertSubscriber<>(true);
        Uni.createFrom().<String> failure(new IOException("boom"))
                .subscribe().withSubscriber(subscriber);
        subscriber.assertNotCompleted();
    }

}
