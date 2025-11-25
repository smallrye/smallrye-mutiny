package io.smallrye.mutiny;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.tuples.Tuple3;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
class BugReproducersTest {

    @RepeatedTest(100)
    void reproducer_689() {
        // Adapted from https://github.com/smallrye/smallrye-mutiny/issues/689
        AtomicLong src = new AtomicLong();

        AssertSubscriber<Long> sub = Multi.createBy().repeating()
                .supplier(src::incrementAndGet)
                .until(l -> l.equals(10_000L))
                .flatMap(l -> Multi.createFrom().item(l * 2))
                .emitOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(9_999);
    }

    @Test
    void reproducer_705() {
        // Adapted from https://github.com/smallrye/smallrye-mutiny/issues/705
        // The issue was an over-interpretation of one of the RS TCK rule regarding releasing subscriber references.
        AssertSubscriber<List<Integer>> sub = AssertSubscriber.create();
        AtomicInteger counter = new AtomicInteger();
        AtomicReference<Throwable> threadFailure = new AtomicReference<>();

        ExecutorService threadPool = Executors.newFixedThreadPool(4, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable task) {
                Thread thread = Executors.defaultThreadFactory().newThread(task);
                thread.setUncaughtExceptionHandler((t, e) -> {
                    e.printStackTrace();
                    threadFailure.set(e);
                });
                return thread;
            }
        });

        Multi.createFrom().range(0, 1000)
                .emitOn(threadPool)
                .group().intoLists().of(100)
                .onItem().invoke(() -> {
                    if (counter.incrementAndGet() == 3) {
                        sub.cancel();
                    }
                })
                .runSubscriptionOn(threadPool)
                .subscribe().withSubscriber(sub);

        sub.request(Long.MAX_VALUE);
        await().atMost(5, TimeUnit.SECONDS).untilAtomic(counter, greaterThanOrEqualTo(3));

        assertThat(threadFailure.get()).isNull();
        sub.assertNotTerminated();
        threadPool.shutdownNow();
    }

    @Test
    void reproducer_quarkus_21528() {
        // From https://github.com/quarkusio/quarkus/issues/21528
        // Generic bounds make the API harder to use than it should be

        Uni<String> c = Uni.createFrom().item("C");
        Uni<String> d = Uni.createFrom().item("D");
        List<Uni<String>> asList = new ArrayList<>();
        asList.add(c);
        asList.add(d);

        Uni.join().first(asList).withItem();
    }

    @Test
    void reproducer_1520() {
        // From https://github.com/smallrye/smallrye-mutiny/issues/1520 and
        // to address https://github.com/quarkusio/quarkus/issues/34613
        AtomicInteger counter = new AtomicInteger();
        Multi.createFrom().iterable(List.of("aa", "bb", "cc"))
                .collect().asList()
                .onTermination().invoke(counter::incrementAndGet)
                .onFailure().retry().withBackOff(Duration.ofSeconds(1)).atMost(2)
                .await().atMost(Duration.ofSeconds(5));
        assertThat(counter).hasValue(1);
    }

    @Test
    void reproducer_1891() {
        // Adapted from https://github.com/smallrye/smallrye-mutiny/issues/1891
        AtomicBoolean completed = new AtomicBoolean();
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        var ticks = Multi.createFrom().ticks().every(Duration.ofMillis(10));
        var data = Multi.createFrom().ticks().every(Duration.ofMillis(15));
        Multi.createBy().combining().streams(ticks, data).latestItems().asTuple()
                .skip().repetitions(Comparator.comparing(Tuple2::getItem1))
                .subscribe().with(
                        item -> {
                            if (counter.incrementAndGet() > 100L) {
                                completed.set(true);
                            }
                        },
                        failure::set);
        await().until(() -> completed.get() || failure.get() != null);
        assertThat(failure.get()).describedAs("No failure must have been emitted").isNull();
        assertThat(completed.get()).isTrue();
    }

    @RepeatedTest(1_000)
    void reproducer_1993() {
        // Race condition in UniAndCombination, spotted in https://github.com/smallrye/smallrye-mutiny/issues/1993
        Uni<String> a = Uni.createFrom().completionStage(() -> CompletableFuture.supplyAsync(() -> "A"));
        Uni<String> b = Uni.createFrom().completionStage(() -> CompletableFuture.supplyAsync(() -> "B"));
        Uni<String> c = Uni.createFrom().completionStage(() -> CompletableFuture.supplyAsync(() -> "C"));
        Uni<Tuple3<String, String, String>> uni = Uni.combine().all().unis(a, b, c).usingConcurrencyOf(3).asTuple();

        UniAssertSubscriber<Tuple3<String, String, String>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
        sub.awaitItem(Duration.ofSeconds(5))
                .assertItem(Tuple3.of("A", "B", "C"));
    }
}
