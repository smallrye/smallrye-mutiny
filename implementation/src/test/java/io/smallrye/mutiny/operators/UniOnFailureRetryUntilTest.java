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

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UniOnFailureRetryUntilTest {

    private final Predicate<Throwable> retryTwice = new Predicate<Throwable>() {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public boolean test(Throwable throwable) {
            int attempt = counter.getAndIncrement();
            return attempt < 2;
        }
    };

    private final Predicate<Throwable> retryOnIoException = throwable -> throwable instanceof IOException;

    @Test
    public void testWithoutFailure() {
        Uni<Integer> upstream = Uni.createFrom().item(1);
        int result = upstream
                .onFailure().retry().until(t -> true)
                .await().indefinitely();
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testInfiniteRetry() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> upstream = Uni.createFrom().emitter(em -> {
            int i = count.incrementAndGet();
            if (i == 1) {
                em.fail(new Exception("boom"));
                return;
            }
            em.complete(1);
        });

        int result = upstream
                .onFailure().retry().until(t -> true)
                .await().indefinitely();
        assertThat(result).isEqualTo(1);
    }

    @Test(expectedExceptions = Exception.class, expectedExceptionsMessageRegExp = ".*boom.*")
    public void testTwoRetriesAndGiveUp() {
        Uni<Integer> upstream = Uni.createFrom().emitter(em -> {
            em.fail(new Exception("boom"));
        });
        upstream
                .onFailure().retry().until(retryTwice)
                .await().indefinitely();
    }

    @Test
    public void testRetryOnSpecificException() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> upstream = Uni.createFrom().emitter(em -> {
            int attempt = count.incrementAndGet();
            if (attempt == 1) {
                em.fail(new IOException("boom"));
            }
            em.complete(2);
        });

        int result = upstream
                .onFailure().retry().until(retryOnIoException).await().indefinitely();
        assertThat(result).isEqualTo(2);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*kaboom.*")
    public void testRetryOnSpecificExceptionAndNotOther() {
        final IOException exception = new IOException("boom");
        final IllegalStateException ise = new IllegalStateException("kaboom");

        AtomicInteger count = new AtomicInteger();
        Uni<Integer> upstream = Uni.createFrom().emitter(em -> {
            int attempt = count.incrementAndGet();
            if (attempt == 1) {
                em.fail(exception);
            }
            em.fail(ise);
        });

        upstream
                .onFailure().retry().until(retryOnIoException)
                .await().indefinitely();
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*boom.*")
    public void testWithPredicateThrowingException() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> upstream = Uni.createFrom().emitter(em -> {
            int i = count.incrementAndGet();
            if (i == 1) {
                em.fail(new Exception("boom"));
                return;
            }
            em.complete(3);
        });

        upstream
                .onFailure().retry().until(t -> {
                    throw new IllegalStateException("boom");
                })
                .await().indefinitely();
    }

    @Test(expectedExceptions = Exception.class, expectedExceptionsMessageRegExp = ".*boom.*")
    public void testWithPredicateReturningFalse() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> upstream = Uni.createFrom().emitter(em -> {
            int i = count.incrementAndGet();
            if (i == 1) {
                em.fail(new Exception("boom"));
                return;
            }
            em.complete(2);
        });

        upstream
                .onFailure().retry().until(t -> false)
                .await().indefinitely();
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*kaboom.*")
    public void testWithPredicateThrowException() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> upstream = Uni.createFrom().emitter(em -> {
            int i = count.incrementAndGet();
            if (i == 1) {
                em.fail(new Exception("boom"));
                return;
            }
            em.complete(2);
        });

        upstream
                .onFailure().retry().until(t -> {
                    throw new RuntimeException("kaboom");
                })
                .await().indefinitely();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testJitterValidation() {
        Uni.createFrom().item(1)
                .onFailure().retry().withJitter(2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatYouCannotUseUntilIfBackoffIsConfigured() {
        Uni.createFrom().item("hello")
                .onFailure().retry().withBackOff(Duration.ofSeconds(1)).until(t -> true);
    }

}
