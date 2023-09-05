package io.smallrye.mutiny.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
public class MutinySchedulerTest {

    @BeforeAll
    public static void init() {
        ExecutorService exec = Executors.newFixedThreadPool(4, new ThreadFactory() {
            final AtomicInteger count = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "my-thread-" + count.getAndIncrement());
            }
        });
        Infrastructure.setDefaultExecutor(exec);
    }

    @AfterAll
    public static void reset() {
        Infrastructure.setDefaultExecutor();
    }

    @Test
    public void testUniRetry() {
        AtomicReference<String> thread = new AtomicReference<>();
        AtomicInteger count = new AtomicInteger();
        String res = Uni.createFrom().<String> emitter(e -> {
            if (count.getAndIncrement() < 2) {
                e.fail(new Exception("boom"));
            } else {
                e.complete("hello");
            }
        })
                .onFailure().retry().withBackOff(Duration.ofNanos(100)).atMost(3)
                .map(s -> {
                    thread.set(Thread.currentThread().getName());
                    return s.toUpperCase();
                })
                .await().indefinitely();

        assertThat(res).isEqualTo("HELLO");
        assertThat(thread).doesNotHaveValue("main").satisfies(ref -> assertThat(ref.get()).startsWith("my-thread-"));
    }

    @Test
    public void testUniRetryExpireIn() {
        AtomicReference<String> thread = new AtomicReference<>();
        AtomicInteger count = new AtomicInteger();
        String res = Uni.createFrom().<String> emitter(e -> {
            if (count.getAndIncrement() < 2) {
                e.fail(new Exception("boom"));
            } else {
                e.complete("hello");
            }
        })
                .onFailure().retry().withBackOff(Duration.ofNanos(100)).expireIn(5_000L)
                .map(s -> {
                    thread.set(Thread.currentThread().getName());
                    return s.toUpperCase();
                })
                .await().indefinitely();

        assertThat(res).isEqualTo("HELLO");
        assertThat(thread).doesNotHaveValue("main").satisfies(ref -> assertThat(ref.get()).startsWith("my-thread-"));
    }

    @Test
    public void testMultiRetry() {
        AtomicReference<String> thread = new AtomicReference<>();
        AtomicInteger count = new AtomicInteger();
        String res = Multi.createFrom().<String> emitter(e -> {
            if (count.getAndIncrement() < 2) {
                e.fail(new Exception("boom"));
            } else {
                e.emit("hello");
                e.complete();
            }
        })
                .onFailure().retry().withBackOff(Duration.ofNanos(100)).atMost(3)
                .map(s -> {
                    thread.set(Thread.currentThread().getName());
                    return s.toUpperCase();
                })
                .collect().first()
                .await().indefinitely();

        assertThat(res).isEqualTo("HELLO");
        assertThat(thread).doesNotHaveValue("main").satisfies(ref -> assertThat(ref.get()).startsWith("my-thread-"));
    }

    @Test
    public void testMultiRetryExpireIn() {
        AtomicReference<String> thread = new AtomicReference<>();
        AtomicInteger count = new AtomicInteger();
        String res = Multi.createFrom().<String> emitter(e -> {
            if (count.getAndIncrement() < 2) {
                e.fail(new Exception("boom"));
            } else {
                e.emit("hello");
                e.complete();
            }
        })
                .onFailure().retry().withBackOff(Duration.ofNanos(100)).expireIn(5_000L)
                .map(s -> {
                    thread.set(Thread.currentThread().getName());
                    return s.toUpperCase();
                })
                .collect().first()
                .await().indefinitely();

        assertThat(res).isEqualTo("HELLO");
        assertThat(thread).doesNotHaveValue("main").satisfies(ref -> assertThat(ref.get()).startsWith("my-thread-"));
    }

    @Test
    public void testTicks() {
        AtomicReference<String> thread = new AtomicReference<>();
        List<Long> list = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .select().first(5)
                .collect().asList()
                .onItem().invoke(l -> thread.set(Thread.currentThread().getName()))
                .await().indefinitely();

        assertThat(list).hasSize(5);
        assertThat(thread.get()).startsWith("my-thread-");
    }

    /**
     * This test verifies that tasks are not kept in the scheduler queue.
     */
    @Test
    public void testTaskCancellation() {
        MutinyScheduler scheduler = (MutinyScheduler) Infrastructure.getDefaultWorkerPool();
        long begin = scheduler.getCompletedTaskCount();
        AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < 1000; i++) {
            Multi.createFrom().ticks().every(Duration.ofMillis(100))
                    .select().first(5)
                    .subscribe().with(x -> {
                        counter.incrementAndGet();
                    });
        }

        await().until(() -> counter.get() == 5000);
        assertThat(scheduler.getCompletedTaskCount()).isEqualTo(begin + 5000);
        assertThat(scheduler.getQueue()).isEmpty();
        assertThat(scheduler.getActiveCount()).isEqualTo(0);
    }

    @Test
    public void testCollectionBasedOnDuration() {
        AtomicReference<String> thread = new AtomicReference<>();
        Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .group().intoLists().every(Duration.ofMillis(10))
                .select().first(5)
                .collect().asList()
                .onItem().invoke(l -> thread.set(Thread.currentThread().getName()))
                .await().indefinitely();
        assertThat(thread.get()).startsWith("my-thread-");
    }

    @Test
    public void testTimeout() {
        AtomicReference<String> thread = new AtomicReference<>();

        Uni.createFrom().emitter(e -> {
            // do nothing
        })
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithItem("hello")
                .onItem().invoke(l -> thread.set(Thread.currentThread().getName()))
                .await().indefinitely();
        assertThat(thread.get()).startsWith("my-thread-");
    }

    @Test
    public void testDelay() {
        List<Uni<String>> list = new ArrayList<>();
        Set<String> threads = new CopyOnWriteArraySet<>();
        for (int i = 0; i < 100; i++) {
            list.add(Uni.createFrom().item("hello")
                    .onItem().delayIt().by(Duration.ofSeconds(1))
                    .onItem().invoke(s -> threads.add(Thread.currentThread().getName())));
        }

        Uni.combine().all().unis(list).with(x -> null).await().indefinitely();
        assertThat(threads).allSatisfy(s -> assertThat(s).startsWith("my-thread-"));
        assertThat(threads).hasSizeLessThanOrEqualTo(4);
    }

    @Test
    public void testSchedulingARunnable() throws InterruptedException, ExecutionException, TimeoutException {
        ScheduledExecutorService pool = Infrastructure.getDefaultWorkerPool();
        assertThat(pool.isShutdown()).isFalse();

        AtomicBoolean executed = new AtomicBoolean();
        RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) pool.schedule(() -> executed.set(true), 1,
                TimeUnit.MILLISECONDS);
        future.get(1, TimeUnit.SECONDS);
        future.get();
        assertThat(executed).isTrue();
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCancelled()).isFalse();
        assertThat(future.isPeriodic()).isFalse();
        assertThat(future.getDelay(TimeUnit.MILLISECONDS)).isLessThanOrEqualTo(0);
    }

    @Test
    public void testSchedulingACallable() throws InterruptedException, ExecutionException, TimeoutException {
        ScheduledExecutorService pool = Infrastructure.getDefaultWorkerPool();
        assertThat(pool.isShutdown()).isFalse();

        AtomicBoolean executed = new AtomicBoolean();
        RunnableScheduledFuture<Integer> future = (RunnableScheduledFuture<Integer>) pool.schedule(() -> {
            executed.set(true);
            return 1;
        }, 1, TimeUnit.MILLISECONDS);
        int r1 = future.get(1, TimeUnit.SECONDS);
        int r2 = future.get();
        assertThat(executed).isTrue();
        assertThat(r1).isEqualTo(r2).isEqualTo(1);
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCancelled()).isFalse();
        assertThat(future.isPeriodic()).isFalse();
        assertThat(future.getDelay(TimeUnit.MILLISECONDS)).isLessThanOrEqualTo(0);
    }

}
