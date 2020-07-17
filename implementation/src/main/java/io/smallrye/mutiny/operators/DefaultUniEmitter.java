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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * Implementation of the Uni Emitter.
 * This implementation makes sure:
 * <ul>
 * <li>only the first event is propagated downstream</li>
 * <li>termination action is called only once and then drop</li>
 * </ul>
 * <p>
 *
 * @param <T> the type of item emitted by the emitter
 */
public class DefaultUniEmitter<T> implements UniEmitter<T>, UniSubscription {

    private final UniSubscriber<T> downstream;
    private final AtomicBoolean disposed = new AtomicBoolean();
    private final AtomicReference<Runnable> onTermination = new AtomicReference<>();

    DefaultUniEmitter(UniSubscriber<T> subscriber) {
        this.downstream = nonNull(subscriber, "subscriber");
    }

    @Override
    public void complete(T item) {
        if (disposed.compareAndSet(false, true)) {
            downstream.onItem(item);
            terminate();
        }
    }

    private void terminate() {
        Runnable runnable = onTermination.getAndSet(null);
        if (runnable != null) {
            runnable.run();
        }
    }

    @Override
    public void fail(Throwable failure) {
        nonNull(failure, "failure");
        if (disposed.compareAndSet(false, true)) {
            downstream.onFailure(failure);
            terminate();
        }
    }

    @Override
    public UniEmitter<T> onTermination(Runnable callback) {
        Runnable actual = nonNull(callback, "callback");
        if (!disposed.get()) {
            this.onTermination.set(actual);
            // Re-check if the termination didn't happen in the meantime
            if (disposed.get()) {
                terminate();
            }
        }
        return this;
    }

    @Override
    public void cancel() {
        if (disposed.compareAndSet(false, true)) {
            terminate();
        }
    }

    public boolean isTerminated() {
        return disposed.get();
    }
}
