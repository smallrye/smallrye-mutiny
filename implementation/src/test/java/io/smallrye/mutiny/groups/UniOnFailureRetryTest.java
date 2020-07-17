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
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniOnFailureRetryTest {
    @Test
    public void testFailureWithPredicateException() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Uni.createFrom().failure(new Throwable("Cause failure"))
                .onFailure(new ThrowablePredicate()).retry().atMost(2)
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);

        await().until(() -> counter.intValue() == 0);
        assertThat(failure.get()).isNotNull();
    }

    @Test
    public void testFailureWithPredicateFailure() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Uni.createFrom().failure(new Throwable("Cause failure"))
                .onFailure((t) -> false).retry().atMost(2)
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);

        await().until(() -> counter.intValue() == 0);
        assertThat(failure.get()).isNotNull();
    }

    @Test
    public void testRetryWhenWithNoFailureInTriggerStream() {
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        AtomicInteger count = new AtomicInteger();
        String value = Uni.createFrom().<String> emitter(e -> {
            int attempt = count.getAndIncrement();
            if (attempt == 0) {
                e.fail(new Exception("boom"));
            } else if (attempt == 1) {
                e.fail(new IOException("another-boom"));
            } else {
                e.complete("done");
            }
        })
                .onFailure().retry().when(stream -> stream.onItem().invoke(failures::add)
                        .onItem()
                        .transformToUni(f -> Uni.createFrom().item("tick").onItem().delayIt().by(Duration.ofMillis(10)))
                        .concatenate())
                .await().atMost(Duration.ofSeconds(5));

        assertThat(value).isEqualTo("done");
        assertThat(failures).hasSize(2)
                .anySatisfy(t -> assertThat(t).hasMessage("boom"))
                .anySatisfy(t -> assertThat(t).isInstanceOf(IOException.class).hasMessage("another-boom"));
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*damned.*")
    public void testRetryWhenWithFailureInTriggerStream() {
        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().<String> emitter(e -> {
            int attempt = count.getAndIncrement();
            if (attempt == 0) {
                e.fail(new Exception("boom"));
            } else if (attempt == 1) {
                e.fail(new IOException("another-boom"));
            } else {
                e.complete("done");
            }
        })
                .onFailure().retry().when(stream -> stream
                        .onItem().transformToUni(f -> Uni.createFrom().failure(new IllegalStateException("damned!")))
                        .concatenate())
                .await().atMost(Duration.ofSeconds(5));
    }

    @Test
    public void testRetryWhenWithCompletionInTriggerStream() {
        AtomicInteger count = new AtomicInteger();
        String value = Uni.createFrom().<String> emitter(e -> {
            int attempt = count.getAndIncrement();
            if (attempt == 0) {
                e.fail(new Exception("boom"));
            } else if (attempt == 1) {
                e.fail(new IOException("another-boom"));
            } else {
                e.complete("done");
            }
        })
                .onFailure().retry().when(stream -> stream.transform().byTakingFirstItems(1))
                .await().atMost(Duration.ofSeconds(5));
        assertThat(value).isNull();
    }

    @Test
    public void testRetryWithBackOff() {
        AtomicInteger count = new AtomicInteger();
        String value = Uni.createFrom().<String> emitter(e -> {
            int attempt = count.getAndIncrement();
            if (attempt == 0) {
                e.fail(new Exception("boom"));
            } else if (attempt == 1) {
                e.fail(new IOException("another-boom"));
            } else {
                e.complete("done");
            }
        })
                .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(1)).withJitter(1.0)
                .atMost(20)
                .await().atMost(Duration.ofSeconds(5));

        assertThat(value).isEqualTo("done");
    }

    @Test
    public void testExpireInRetryWithBackOff() {
        AtomicInteger count = new AtomicInteger();
        String value = Uni.createFrom().<String> emitter(e -> {
            int attempt = count.getAndIncrement();
            if (attempt == 0) {
                e.fail(new Exception("boom"));
            } else if (attempt == 1) {
                e.fail(new IOException("another-boom"));
            } else {
                e.complete("done");
            }
        })
                .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(1)).withJitter(1.0)
                .expireIn(10_000L)
                .await().atMost(Duration.ofSeconds(5));

        assertThat(value).isEqualTo("done");
    }

    @Test
    public void testExpireAtRetryWithBackOff() {
        AtomicInteger count = new AtomicInteger();
        String value = Uni.createFrom().<String> emitter(e -> {
            int attempt = count.getAndIncrement();
            if (attempt == 0) {
                e.fail(new Exception("boom"));
            } else if (attempt == 1) {
                e.fail(new IOException("another-boom"));
            } else {
                e.complete("done");
            }
        })
                .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(1)).withJitter(1.0)
                .expireAt(System.currentTimeMillis() + 10_000L)
                .await().atMost(Duration.ofSeconds(5));

        assertThat(value).isEqualTo("done");
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*4/4.*")
    public void testRetryWithBackOffReachingMaxAttempt() {
        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().<String> emitter(e -> {
            e.fail(new Exception("boom " + count.getAndIncrement()));
        })
                .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(20)).withJitter(1.0)
                .atMost(4)
                .await().atMost(Duration.ofSeconds(5));
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".* attempts.*")
    public void testRetryWithBackOffReachingExpiresIn() {
        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().<String> emitter(e -> {
            e.fail(new Exception("boom " + count.getAndIncrement()));
        })
                .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(20)).withJitter(1.0)
                .expireIn(90L)
                .await().atMost(Duration.ofSeconds(5));
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".* attempts.*")
    public void testRetryWithBackOffReachingExpiresAt() {
        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().<String> emitter(e -> {
            e.fail(new Exception("boom " + count.getAndIncrement()));
        })
                .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofSeconds(20)).withJitter(1.0)
                .expireAt(System.currentTimeMillis() + 90L)
                .await().atMost(Duration.ofSeconds(5));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatYouCannotUseWhenIfBackoffIsConfigured() {
        Uni.createFrom().item("hello")
                .onFailure().retry().withBackOff(Duration.ofSeconds(1)).when(t -> Multi.createFrom().item(t));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExpireAtThatYouCannotUseWhenIfBackoffIsNotConfigured() {
        Uni.createFrom().item("hello")
                .onFailure().retry().expireAt(1L);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExpireInThatYouCannotUseWhenIfBackoffIsNotConfigured() {
        Uni.createFrom().item("hello")
                .onFailure().retry().expireIn(1L);
    }

    static class ThrowablePredicate implements Predicate<Throwable> {
        @Override
        public boolean test(Throwable throwable) {
            throw new RuntimeException();
        }
    }

}
