package io.smallrye.mutiny.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MutinySchedulerTest {

    @BeforeClass
    public static void init() {
        ExecutorService exec = Executors.newFixedThreadPool(4, new ThreadFactory() {
            AtomicInteger count = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "my-thread-" + count.getAndIncrement());
            }
        });
        Infrastructure.setDefaultExecutor(exec);
    }

    @AfterClass
    public static void reset() {
        Executor current = Infrastructure.getDefaultExecutor();
        if (current instanceof ExecutorService) {
            ((ExecutorService) current).shutdownNow();
        }
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
                .collectItems().first()
                .await().indefinitely();

        assertThat(res).isEqualTo("HELLO");
        assertThat(thread).doesNotHaveValue("main").satisfies(ref -> assertThat(ref.get()).startsWith("my-thread-"));
    }

    @Test
    public void testTicks() {
        AtomicReference<String> thread = new AtomicReference<>();
        List<Long> list = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .transform().byTakingFirstItems(5)
                .collectItems().asList()
                .onItem().invoke(l -> thread.set(Thread.currentThread().getName()))
                .await().indefinitely();

        assertThat(list).hasSize(5);
        assertThat(thread.get()).startsWith("my-thread-");
    }

    @Test
    public void testCollectionBasedOnDuration() {
        AtomicReference<String> thread = new AtomicReference<>();
        Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .groupItems().intoLists().every(Duration.ofMillis(10))
                .transform().byTakingFirstItems(5)
                .collectItems().asList()
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

        Uni.combine().all().unis(list).combinedWith(x -> null).await().indefinitely();
        assertThat(threads).allSatisfy(s -> assertThat(s).startsWith("my-thread-"));
        assertThat(threads).hasSizeLessThanOrEqualTo(4);
    }

}
