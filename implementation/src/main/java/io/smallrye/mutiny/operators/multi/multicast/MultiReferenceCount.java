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

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * A {@link Multi} stays connected to the source as long as there is at least one subscription.
 *
 * @param <T> the type of item
 */
public class MultiReferenceCount<T> extends AbstractMulti<T> implements Multi<T> {

    private final ConnectableMulti<T> upstream;
    private final int numberOfSubscribers;
    private final Duration duration;
    private final ScheduledExecutorService executor;

    private ConnectableMultiConnection connection;

    public MultiReferenceCount(ConnectableMulti<T> upstream) {
        this(upstream, 1, null);
    }

    public MultiReferenceCount(ConnectableMulti<T> upstream, int numberOfSubscribers, Duration duration) {
        this.upstream = upstream;
        this.numberOfSubscribers = numberOfSubscribers;
        this.duration = duration;
        this.executor = Infrastructure.getDefaultWorkerPool();
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> s) {
        ConnectableMultiConnection conn;
        boolean connect;
        synchronized (this) {
            conn = connection;
            if (conn == null) {
                conn = new ConnectableMultiConnection(this);
                connection = conn;
            }

            conn.cancelTimerIf0();
            connect = conn.shouldConnectAfterIncrement(numberOfSubscribers);
        }

        upstream.subscribe().withSubscriber(new MultiReferenceCountSubscriber<>(s, this, conn));

        if (connect) {
            upstream.connect(conn);
        }
    }

    void cancel(ConnectableMultiConnection connection) {
        synchronized (this) {
            if (this.connection == null || this.connection != connection) {
                return;
            }
            long count = connection.decrement();
            if (count != 0L || !connection.isConnected()) {
                return;
            }

            if (duration == null || duration.toMillis() == 0L) {
                timeout(connection);
                return;
            }
        }

        ScheduledFuture<?> future = executor.schedule(connection, duration.toMillis(), TimeUnit.MILLISECONDS);
        connection.setTimer(() -> future.cancel(true));
    }

    void terminated(ConnectableMultiConnection connection) {
        synchronized (this) {
            if (this.connection != null && this.connection == connection) {
                this.connection = null;
                connection.cancel();
            }
            if (connection.decrementAndReached0()) {
                if (upstream instanceof Cancellable) {
                    ((Cancellable) upstream).cancel();
                }
            }
        }
    }

    void timeout(ConnectableMultiConnection connection) {
        synchronized (this) {
            if (connection.getSubscriberCount() == 0 && connection == this.connection) {
                this.connection = null;
                connection.cancel();
                if (upstream instanceof Cancellable) {
                    ((Cancellable) upstream).cancel();
                }
            }
        }
    }

}
