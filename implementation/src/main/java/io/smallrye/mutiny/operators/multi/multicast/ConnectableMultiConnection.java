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
package io.smallrye.mutiny.operators.multi.multicast;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.smallrye.mutiny.subscription.Cancellable;

public class ConnectableMultiConnection implements Runnable, Consumer<Cancellable> {

    private static final Cancellable CANCELLED = () -> {
        // do nothing.
    };

    private final MultiReferenceCount<?> parent;
    private final AtomicReference<Cancellable> onCancellation = new AtomicReference<>();

    private Cancellable timer;
    private long subscriberCount;
    private boolean connected;

    ConnectableMultiConnection(MultiReferenceCount<?> parent) {
        this.parent = parent;
    }

    @Override
    public void run() {
        parent.timeout(this);
    }

    @Override
    public void accept(Cancellable action) {
        for (;;) {
            Cancellable current = onCancellation.get();
            if (current == CANCELLED) {
                if (action != null) {
                    action.cancel();
                }
            }
            if (onCancellation.compareAndSet(current, action)) {
                break;
            }
        }
    }

    public synchronized boolean shouldConnectAfterIncrement(int toBeReached) {
        subscriberCount = subscriberCount + 1;
        if (!connected && subscriberCount == toBeReached) {
            connected = true;
            return true;
        } else {
            return false;
        }
    }

    public long getSubscriberCount() {
        return subscriberCount;
    }

    public boolean isConnected() {
        return connected;
    }

    public void cancelTimerIf0() {
        boolean cancel;
        synchronized (this) {
            cancel = subscriberCount == 0L && timer != null;
        }
        if (cancel) {
            timer.cancel();
        }
    }

    public void cancel() {
        Cancellable current = onCancellation.getAndSet(CANCELLED);
        if (current != null && current != CANCELLED) {
            current.cancel();
        }
    }

    synchronized boolean decrementAndReached0() {
        if (subscriberCount == 1) {
            subscriberCount = 0;
            return true;
        } else {
            subscriberCount--;
            return false;
        }
    }

    synchronized long decrement() {
        subscriberCount = subscriberCount - 1;
        return subscriberCount;
    }

    synchronized void setTimer(Cancellable cancellable) {
        if (timer != null && timer != CANCELLED) {
            timer.cancel();
        }
        this.timer = cancellable;
    }
}
