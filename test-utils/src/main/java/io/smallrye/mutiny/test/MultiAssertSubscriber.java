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
package io.smallrye.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@SuppressWarnings("SubscriberImplementation")
public class MultiAssertSubscriber<T> implements Subscriber<T> {

    private final CountDownLatch latch = new CountDownLatch(1);

    private AtomicReference<Subscription> subscription = new AtomicReference<>();

    private AtomicLong requested = new AtomicLong();

    private List<T> items = new CopyOnWriteArrayList<>();
    private List<Throwable> failures = new CopyOnWriteArrayList<>();

    private int numberOfSubscription = 0;

    private int numberOfCompletionEvents = 0;
    private boolean upfrontCancellation;

    public MultiAssertSubscriber(long requested, boolean cancelled) {
        this.requested.set(requested);
        upfrontCancellation = cancelled;
    }

    public MultiAssertSubscriber(long requested) {
        this(requested, false);
    }

    public static <T> MultiAssertSubscriber<T> create() {
        return new MultiAssertSubscriber<>(0);
    }

    public static <T> MultiAssertSubscriber<T> create(long requested) {
        return new MultiAssertSubscriber<>(requested);
    }

    public MultiAssertSubscriber<T> assertCompletedSuccessfully() {
        assertHasNotFailed();
        int num = numberOfCompletionEvents;
        if (num == 0) {
            throw new AssertionError("Not yet completed");
        }
        if (num > 1) {
            throw new AssertionError("Too many completions: " + num);
        }
        return this;
    }

    public MultiAssertSubscriber<T> assertHasFailedWith(Class<? extends Throwable> typeOfException, String message) {
        assertHasNotCompleted();
        int count = failures.size();
        if (count == 0) {
            throw new AssertionError("The multi didn't failed");
        }
        if (count > 1) {
            throw new AssertionError("The multi emitted several failure events errors: " + count);
        }

        Throwable throwable = failures.get(0);
        assertThat(throwable).isInstanceOf(typeOfException);
        if (message != null) {
            assertThat(throwable).hasMessageContaining(message);
        }

        return this;
    }

    public MultiAssertSubscriber<T> assertHasNotFailed() {
        assertThat(failures).hasSize(0);
        return this;
    }

    public MultiAssertSubscriber<T> assertHasNotReceivedAnyItem() {
        assertThat(items).isEmpty();
        return this;
    }

    public MultiAssertSubscriber<T> assertHasNotCompleted() {
        assertThat(numberOfCompletionEvents).isEqualTo(0);
        return this;
    }

    public MultiAssertSubscriber<T> assertSubscribed() {
        assertThat(numberOfSubscription).isEqualTo(1);
        return this;
    }

    public MultiAssertSubscriber<T> assertNotSubscribed() {
        assertThat(numberOfSubscription).isEqualTo(0);
        return this;
    }

    public MultiAssertSubscriber<T> assertTerminated() {
        assertThat(latch.getCount()).isEqualTo(0);
        return this;
    }

    public MultiAssertSubscriber<T> assertNotTerminated() {
        assertThat(latch.getCount()).as("Multi did not complete yet").isGreaterThan(0);
        return this;
    }

    @SafeVarargs
    public final MultiAssertSubscriber<T> assertReceived(T... expected) {
        assertThat(items).containsExactly(expected);
        return this;
    }

    public MultiAssertSubscriber<T> await() {
        if (latch.getCount() == 0) {
            // We are done already.
            return this;
        }

        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return this;
    }

    public MultiAssertSubscriber<T> await(Duration duration) {
        if (latch.getCount() == 0) {
            // We are done already.
            return this;
        }

        try {
            if (!latch.await(duration.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new AssertionError("Not terminated before timeout");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return this;
    }

    public MultiAssertSubscriber<T> cancel() {
        assertThat(subscription.get()).as("No subscription").isNotNull();
        subscription.get().cancel();
        return this;
    }

    public MultiAssertSubscriber<T> request(long req) {
        requested.addAndGet(req);
        if (subscription.get() != null) {
            subscription.get().request(req);
        }
        return this;
    }

    @Override
    public void onSubscribe(Subscription s) {
        numberOfSubscription++;
        subscription.set(s);
        if (upfrontCancellation) {
            s.cancel();
        }
        if (requested.get() > 0) {
            s.request(requested.get());
        }

    }

    @Override
    public synchronized void onNext(T t) {
        items.add(t);
    }

    @Override
    public void onError(Throwable t) {
        failures.add(t);
        latch.countDown();
    }

    @Override
    public void onComplete() {
        numberOfCompletionEvents++;
        latch.countDown();
    }

    public List<T> items() {
        return items;
    }

    public List<Throwable> failures() {
        return failures;
    }

    public MultiAssertSubscriber<T> run(Runnable action) {
        try {
            action.run();
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
        return this;
    }
}
