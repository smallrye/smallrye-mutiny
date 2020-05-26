package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class UniRunSubscriptionOnTest {

    @Test
    public void testRunSubscriptionOnWithSupplier() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni.createFrom().item(() -> 1)
                .runSubscriptionOn(ForkJoinPool.commonPool())
                .subscribe().withSubscriber(ts);
        ts.await().assertItem(1);
        assertThat(ts.getOnSubscribeThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    public void testWithWithImmediateValue() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        Uni.createFrom().item(1)
                .runSubscriptionOn(ForkJoinPool.commonPool())
                .subscribe().withSubscriber(ts);

        ts.await().assertItem(1);
        assertThat(ts.getOnSubscribeThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    public void testWithTimeout() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        Uni.createFrom().item(() -> {
            try {
                TimeUnit.SECONDS.sleep(2L);
            } catch (InterruptedException e) {
                // ignored
            }
            return 0;
        })
                .ifNoItem().after(Duration.ofMillis(100)).recoverWithUni(Uni.createFrom().item(() -> 1))
                // Should not use the default as in container you may have a single thread, blocked by the sleep statement.
                .runSubscriptionOn(executorService)
                .subscribe().withSubscriber(ts);

        ts.await().assertItem(1);

        executorService.shutdownNow();
    }

    @Test
    public void callableEvaluatedTheRightTime() {
        AtomicInteger count = new AtomicInteger();

        Uni<Integer> uni = Uni.createFrom().item(count::incrementAndGet)
                .runSubscriptionOn(ForkJoinPool.commonPool());

        assertThat(count).hasValue(0);
        uni.subscribe().withSubscriber(UniAssertSubscriber.create()).await();
        assertThat(count).hasValue(1);
    }

    @Test
    public void testWithFailure() {
        Uni.createFrom().<Void> failure(new IOException("boom"))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertFailure(IOException.class, "boom");
    }

}
