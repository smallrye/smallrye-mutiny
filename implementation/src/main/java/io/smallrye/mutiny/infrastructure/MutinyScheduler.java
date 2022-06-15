package io.smallrye.mutiny.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return super.schedule(Infrastructure.decorate(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return super.schedule(Infrastructure.decorate(callable), delay, unit);
    }

    // Not overriding the `submit` methods from `ScheduledThreadPoolExecutor` as they delegate to `schedule`,
    // hence we could have double interceptors.

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return super.scheduleAtFixedRate(Infrastructure.decorate(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return super.scheduleWithFixedDelay(Infrastructure.decorate(command), initialDelay, delay, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return super.invokeAny(decorateTasks(tasks));
    }

    private <T> List<Callable<T>> decorateTasks(Collection<? extends Callable<T>> tasks) {
        return tasks
                .stream()
                .map(Infrastructure::decorate)
                .collect(Collectors.toList());
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return super.invokeAny(decorateTasks(tasks), timeout, unit);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return super.invokeAll(decorateTasks(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return super.invokeAll(decorateTasks(tasks), timeout, unit);
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
            if (!isCancelled()) {
                executor.execute(origin);
            }
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
        public V get() throws ExecutionException, InterruptedException {
            return origin.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return origin.get(timeout, unit);
        }
    }

}
