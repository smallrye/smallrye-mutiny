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
package io.smallrye.mutiny.operators.multi.builders;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class IntervalMulti extends AbstractMulti<Long> {

    private final ScheduledExecutorService executor;
    private final Duration initialDelay;
    private final Duration period;

    public IntervalMulti(
            Duration initialDelay,
            Duration period,
            ScheduledExecutorService executor) {
        this.initialDelay = ParameterValidation.validate(initialDelay, "initialDelay");
        this.period = ParameterValidation.validate(period, "period");
        this.executor = ParameterValidation.nonNull(executor, "executor");
    }

    public IntervalMulti(
            Duration period,
            ScheduledExecutorService executor) {
        this.initialDelay = null;
        this.period = ParameterValidation.validate(period, "period");
        this.executor = ParameterValidation.nonNull(executor, "executor");
    }

    @Override
    public void subscribe(MultiSubscriber<? super Long> actual) {
        IntervalRunnable runnable = new IntervalRunnable(actual);

        actual.onSubscribe(runnable);

        try {
            if (initialDelay != null) {
                executor.scheduleAtFixedRate(runnable, initialDelay.toMillis(), period.toMillis(),
                        TimeUnit.MILLISECONDS);
            } else {
                executor.scheduleAtFixedRate(runnable, 0, period.toMillis(),
                        TimeUnit.MILLISECONDS);
            }
        } catch (RejectedExecutionException ree) {
            if (!runnable.cancelled.get()) {
                actual.onFailure(new RejectedExecutionException(ree));
            }
        }
    }

    static final class IntervalRunnable implements Runnable, Subscription {
        private final MultiSubscriber<? super Long> actual;
        private final AtomicLong requested = new AtomicLong();
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private final AtomicLong count = new AtomicLong();

        IntervalRunnable(MultiSubscriber<? super Long> actual) {
            this.actual = actual;
        }

        @Override
        public void run() {
            if (!cancelled.get()) {
                if (requested.get() != 0L) {
                    actual.onItem(count.getAndIncrement());
                    if (requested.get() != Long.MAX_VALUE) {
                        requested.decrementAndGet();
                    }
                } else {
                    cancel();
                    actual.onFailure(
                            new BackPressureFailure("Could not emit tick " + count + " due to lack of requests"));
                }
            }
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
            }
        }

        @Override
        public void cancel() {
            cancelled.set(true);
        }
    }
}
