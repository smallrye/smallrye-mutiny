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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiOnFailureRetryUntilTest {

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
        Multi<Integer> upstream = Multi.createFrom().range(0, 4);
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);
        upstream
                .onFailure().retry().until(t -> true)
                .subscribe().withSubscriber(subscriber);
        subscriber.assertReceived(0, 1, 2, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testInfiniteRetry() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> upstream = Multi.createFrom().emitter(em -> {
            int i = count.incrementAndGet();
            em.emit(0);
            em.emit(1);
            if (i == 1) {
                em.fail(new Exception("boom"));
                return;
            }
            em.emit(2);
            em.emit(3);
            em.complete();
        });

        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);
        upstream
                .onFailure().retry().until(t -> true)
                .subscribe(subscriber);

        subscriber.assertReceived(0, 1, 0, 1, 2, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testTwoRetriesAndGiveUp() {
        Multi<Integer> upstream = Multi.createFrom().emitter(em -> {
            em.emit(0);
            em.emit(1);
            em.fail(new Exception("boom"));
        });
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);

        upstream
                .onFailure().retry().until(retryTwice)
                .subscribe().withSubscriber(subscriber);

        subscriber.assertReceived(0, 1, 0, 1, 0, 1).assertHasFailedWith(Exception.class, "boom");
    }

    @Test
    public void testRetryOnSpecificException() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> upstream = Multi.createFrom().emitter(em -> {
            int attempt = count.incrementAndGet();
            em.emit(0);
            em.emit(1);
            if (attempt == 1) {
                em.fail(new IOException("boom"));
            }
            em.emit(2);
            em.emit(3);
            em.complete();
        });

        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);
        upstream
                .onFailure().retry().until(retryOnIoException).subscribe().withSubscriber(subscriber);

        subscriber
                .assertReceived(0, 1, 0, 1, 2, 3)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testRetryOnSpecificExceptionAndNotOther() {
        final IOException exception = new IOException("boom");
        final IllegalStateException ise = new IllegalStateException("kaboom");

        AtomicInteger count = new AtomicInteger();
        Multi<Integer> upstream = Multi.createFrom().emitter(em -> {
            int attempt = count.incrementAndGet();
            em.emit(0);
            em.emit(1);
            if (attempt == 1) {
                em.fail(exception);
            }
            em.emit(2);
            em.emit(3);
            em.fail(ise);
        });
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);
        upstream
                .onFailure().retry().until(retryOnIoException)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertHasFailedWith(IllegalStateException.class, "kaboom")
                .assertReceived(0, 1, 0, 1, 2, 3);
    }

    @Test
    public void testUnsubscribeFromRetry() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();

        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().publisher(processor)
                .onFailure().retry().until(retryTwice)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        processor.onNext(1);
        subscriber.cancel();
        processor.onNext(2);
        assertThat(subscriber.items()).hasSize(1);
        subscriber.assertNotTerminated();
    }

    @Test
    public void testWithPredicateThrowingException() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> upstream = Multi.createFrom().emitter(em -> {
            int i = count.incrementAndGet();
            em.emit(0);
            em.emit(1);
            if (i == 1) {
                em.fail(new Exception("boom"));
                return;
            }
            em.emit(2);
            em.emit(3);
            em.complete();
        });

        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);
        upstream
                .onFailure().retry().until(t -> {
                    throw new IllegalStateException("boom");
                })
                .subscribe().withSubscriber(subscriber);
        subscriber.assertReceived(0, 1).assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithPredicateReturningFalse() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> upstream = Multi.createFrom().emitter(em -> {
            int i = count.incrementAndGet();
            em.emit(0);
            em.emit(1);
            if (i == 1) {
                em.fail(new Exception("boom"));
                return;
            }
            em.emit(2);
            em.emit(3);
            em.complete();
        });

        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);
        upstream
                .onFailure().retry().until(t -> false)
                .subscribe().withSubscriber(subscriber);
        subscriber.assertReceived(0, 1).assertHasFailedWith(Exception.class, "boom");
    }

}
