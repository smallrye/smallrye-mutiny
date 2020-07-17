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
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class MultiSubscribeTest {

    @Test
    public void testSubscribeWithItemAndFailure() {
        List<Long> items = new CopyOnWriteArrayList<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Cancellable cancellable = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .subscribe().with(items::add, failure::set);

        await().until(() -> items.size() > 5);
        cancellable.cancel();

        int s = items.size();
        assertThat(items).contains(1L, 2L, 3L, 4L, 5L);
        assertThat(failure.get()).isNull();

        await().pollDelay(10, TimeUnit.MILLISECONDS).until(() -> items.size() == s);

    }

    @Test
    public void testSubscribeWithItemFailureAndCompletion() {
        List<Long> items = new CopyOnWriteArrayList<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .transform().byTakingFirstItems(10)
                .subscribe().with(items::add, failure::set, () -> completion.set(true));

        await().until(() -> items.size() > 5);
        await().until(completion::get);
        assertThat(items).contains(1L, 2L, 3L, 4L, 5L);
        assertThat(failure.get()).isNull();
        assertThat(completion).isTrue();
    }

    @Test
    public void testWithEmitterWithCompletion() {
        List<Integer> items = new CopyOnWriteArrayList<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi.createFrom().<Integer> emitter(emitter::set)
                .subscribe().with(items::add, failure::set, () -> completion.set(true));

        assertThat(items).isEmpty();
        assertThat(failure.get()).isNull();
        assertThat(completion).isFalse();

        emitter.get().emit(1).emit(2).emit(3).complete();

        assertThat(items).containsExactly(1, 2, 3);
        assertThat(failure.get()).isNull();
        assertThat(completion).isTrue();
    }

    @Test
    public void testWithEmitterWithFailure() {
        List<Integer> items = new CopyOnWriteArrayList<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi.createFrom().<Integer> emitter(emitter::set)
                .subscribe().with(items::add, failure::set, () -> completion.set(true));

        assertThat(items).isEmpty();
        assertThat(failure.get()).isNull();
        assertThat(completion).isFalse();

        emitter.get().emit(1).emit(2).emit(3).fail(new IOException("boom"));

        assertThat(items).containsExactly(1, 2, 3);
        assertThat(failure.get()).isInstanceOf(IOException.class).hasMessage("boom");
        assertThat(completion).isFalse();
    }

    @Test
    public void testWith2CallbacksAndFailure() {
        List<Integer> items = new CopyOnWriteArrayList<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi.createFrom().<Integer> emitter(emitter::set)
                .subscribe().with(items::add, failure::set);

        assertThat(items).isEmpty();
        assertThat(failure.get()).isNull();

        emitter.get().emit(1).emit(2).emit(3).fail(new IOException("boom"));

        assertThat(items).containsExactly(1, 2, 3);
        assertThat(failure.get()).isInstanceOf(IOException.class).hasMessage("boom");
    }

    @Test
    public void testWith2CallbacksAndCompletion() {
        List<Integer> items = new CopyOnWriteArrayList<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi.createFrom().<Integer> emitter(emitter::set)
                .subscribe().with(items::add, () -> completion.set(true));

        assertThat(items).isEmpty();
        assertThat(completion).isFalse();

        emitter.get().emit(1).emit(2).emit(3).complete();

        assertThat(items).containsExactly(1, 2, 3);
        assertThat(completion).isTrue();
    }

    @Test
    public void testWith4CallbacksAndCancellation() {
        List<Integer> items = new CopyOnWriteArrayList<>();
        AtomicBoolean completion = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Cancellable cancellable = Multi.createFrom().range(1, 100)
                .subscribe().with(s -> s.request(3), items::add, failure::set, () -> completion.set(true));

        cancellable.cancel();

        // Called twice
        cancellable.cancel();

        assertThat(items).contains(1, 2, 3);
        assertThat(failure.get()).isNull();
        assertThat(completion).isFalse();
    }

}
