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
package io.smallrye.mutiny.infrastructure;

import java.util.concurrent.*;

/**
 * Implementation of {@link ScheduledThreadPoolExecutor} delegating the execution of the task to a configured
 * {@link Executor}.
 *
 * Important: {@link RunnableScheduledFuture#get()} and {@link RunnableScheduledFuture#get(long, TimeUnit)} are not
 * supported.
 */
public class MutinyScheduler extends ScheduledThreadPoolExecutor {

    private final Executor executor;

    public MutinyScheduler(Executor executor) {
        super(1);
        this.executor = executor;
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        return new DecoratedRunnableTask<>(task, executor);
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
        return new DecoratedRunnableTask<>(task, executor);
    }

    static class DecoratedRunnableTask<V> implements RunnableScheduledFuture<V> {

        private final Executor executor;
        private final RunnableScheduledFuture<V> origin;

        public DecoratedRunnableTask(RunnableScheduledFuture<V> origin, Executor executor) {
            this.origin = origin;
            this.executor = executor;
        }

        @Override
        public boolean isPeriodic() {
            return origin.isPeriodic();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return origin.getDelay(unit);
        }

        @Override
        public int compareTo(Delayed o) {
            return origin.compareTo(o);
        }

        @Override
        public void run() {
            executor.execute(origin);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return origin.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return origin.isCancelled();
        }

        @Override
        public boolean isDone() {
            return origin.isDone();
        }

        @Override
        public V get() {
            throw new UnsupportedOperationException();
        }

        @Override
        public V get(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
    }

}
