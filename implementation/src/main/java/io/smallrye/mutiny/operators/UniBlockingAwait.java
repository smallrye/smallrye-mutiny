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

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniBlockingAwait {

    private UniBlockingAwait() {
        // Avoid direct instantiation.
    }

    public static <T> T await(Uni<T> upstream, Duration duration) {
        nonNull(upstream, "upstream");
        validate(duration);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> reference = new AtomicReference<>();
        AtomicReference<Throwable> referenceToFailure = new AtomicReference<>();
        UniSubscriber<T> subscriber = new UniSubscriber<T>() {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                // Do nothing.
            }

            @Override
            public void onItem(T item) {
                reference.set(item);
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable failure) {
                referenceToFailure.compareAndSet(null, failure);
                latch.countDown();
            }
        };
        AbstractUni.subscribe(upstream, subscriber);
        try {
            if (duration != null) {
                if (!latch.await(duration.toMillis(), TimeUnit.MILLISECONDS)) {
                    referenceToFailure.compareAndSet(null, new TimeoutException());
                }
            } else {
                latch.await();
            }
        } catch (InterruptedException e) {
            referenceToFailure.compareAndSet(null, e);
            Thread.currentThread().interrupt();
        }

        Throwable throwable = referenceToFailure.get();
        if (throwable != null) {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            throw new CompletionException(throwable);
        } else {
            return reference.get();
        }
    }

    private static void validate(Duration duration) {
        if (duration == null) {
            return;
        }
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("`duration` must be greater than zero");
        }
    }
}
