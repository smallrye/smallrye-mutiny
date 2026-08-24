package io.smallrye.mutiny.operators.multi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Delayed;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;

/**
 * Reproducers for <a href="https://github.com/smallrye/smallrye-mutiny/issues/2187">#2187</a>:
 * the size-based flush (in {@code onItem}) and the timer-based flush must not both act on the
 * same buffer window.
 *
 * The timers are driven manually so the problematic interleavings are deterministic: invoking a
 * captured runnable after its task was cancelled with {@code cancel(false)} models a timer that
 * already started running when the cancellation arrived.
 */
class MultiBufferWithTimeoutOpTest {

    @Test
    void staleTimerMustNotEmitPartialWindowAfterSizeFlush() {
        ManualScheduler scheduler = new ManualScheduler();
        AssertSubscriber<List<Integer>> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

        MultiBufferWithTimeoutOp.MultiBufferWithTimeoutProcessor<Integer> processor = processor(scheduler, subscriber,
                2, false);

        processor.onSubscribe(emptySubscription());
        processor.request(Long.MAX_VALUE);

        processor.onItem(1); // arms the timeout task T0
        processor.onItem(2); // size boundary: cancels T0 and emits [1, 2]
        assertThat(subscriber.getItems()).containsExactly(List.of(1, 2));

        processor.onItem(3); // arms the timeout task T1

        // T0 fires even though it has been cancelled: cancel(false) does not stop an
        // already-running timer, so this emulates the raced interleaving.
        scheduler.run(0);

        // The stale timer must not flush the partial window [3].
        assertThat(subscriber.getItems()).containsExactly(List.of(1, 2));
    }

    @Test
    void staleTimerMustNotEmitEmptyWindowAfterSizeFlushWhenEmitEmptyEnabled() {
        ManualScheduler scheduler = new ManualScheduler();
        AssertSubscriber<List<Integer>> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

        MultiBufferWithTimeoutOp.MultiBufferWithTimeoutProcessor<Integer> processor = processor(scheduler, subscriber,
                2, true);

        processor.onSubscribe(emptySubscription()); // schedules S0 right away
        processor.request(Long.MAX_VALUE);

        processor.onItem(1);
        scheduler.run(0); // S0 fires on time: emits [1] and schedules S1
        assertThat(subscriber.getItems()).containsExactly(List.of(1));

        processor.onItem(2);
        processor.onItem(3); // size boundary: cancels S1 and emits [2, 3], schedules S2
        assertThat(subscriber.getItems()).containsExactly(List.of(1), List.of(2, 3));

        Runnable s1 = scheduler.task(1);
        s1.run(); // S1 fires even though it has been cancelled at the size boundary

        // The stale timer must neither emit an empty group nor schedule an extra task.
        assertThat(subscriber.getItems()).containsExactly(List.of(1), List.of(2, 3));
        assertThat(scheduler.taskCount()).isEqualTo(3); // S0, S1, S2 - no extra scheduling
    }

    private static MultiBufferWithTimeoutOp.MultiBufferWithTimeoutProcessor<Integer> processor(
            ManualScheduler scheduler, AssertSubscriber<List<Integer>> subscriber, int size, boolean emitEmpty) {
        return new MultiBufferWithTimeoutOp.MultiBufferWithTimeoutProcessor<>(
                subscriber, size, Duration.ofMillis(100), scheduler, ArrayList::new, emitEmpty);
    }

    private static Subscription emptySubscription() {
        return new Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        };
    }

    /**
     * A scheduler that never fires anything on its own; tests decide when (and in which order)
     * the scheduled tasks run, which makes the race interleavings deterministic.
     */
    static final class ManualScheduler extends AbstractExecutorService implements ScheduledExecutorService {

        private final List<Runnable> tasks = new CopyOnWriteArrayList<>();
        private final List<ScheduledFuture<?>> futures = new CopyOnWriteArrayList<>();

        void run(int index) {
            tasks.get(index).run();
        }

        Runnable task(int index) {
            return tasks.get(index);
        }

        int taskCount() {
            return tasks.size();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            tasks.add(command);
            FakeScheduledFuture future = new FakeScheduledFuture();
            futures.add(future);
            return future;
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void execute(Runnable command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
    }

    static final class FakeScheduledFuture implements ScheduledFuture<Object> {

        private final AtomicBoolean cancelled = new AtomicBoolean();

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled.set(true);
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public boolean isDone() {
            return cancelled.get();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(Delayed other) {
            return 0;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    }
}
