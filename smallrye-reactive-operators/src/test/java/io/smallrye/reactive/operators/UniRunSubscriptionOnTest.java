package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.helpers.Infrastructure;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class UniRunSubscriptionOnTest {

    @Test
    public void testSubscribeOnWithSupplier() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni.createFrom().result(() -> 1)
                .callSubscribeOn(ForkJoinPool.commonPool())
                .subscribe().withSubscriber(ts);
        ts.await().assertResult(1);
        assertThat(ts.getOnSubscribeThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    public void testWithWithImmediateValue() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        Uni.createFrom().result(1)
                .callSubscribeOn(ForkJoinPool.commonPool())
                .subscribe().withSubscriber(ts);

        ts.await().assertResult(1);
        assertThat(ts.getOnSubscribeThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    public void testWithTimeout() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        Uni.createFrom().result(() -> {
            try {
                TimeUnit.SECONDS.sleep(1L);
            } catch (InterruptedException e) {
                // ignored
            }
            return 0;
        })
                .onNoResult().after(Duration.ofMillis(100)).recoverWithUni(Uni.createFrom().result(() -> 1))
                .callSubscribeOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(ts);

        ts.await().assertResult(1);
    }

    @Test
    public void callableEvaluatedTheRightTime() {
        AtomicInteger count = new AtomicInteger();

        Uni<Integer> uni = Uni.createFrom().result(count::incrementAndGet)
                .callSubscribeOn(ForkJoinPool.commonPool());

        assertThat(count).hasValue(0);
        uni.subscribe().withSubscriber(UniAssertSubscriber.create()).await();
        assertThat(count).hasValue(1);
    }

    @Test
    public void testWithFailure() {
        Uni.createFrom().<Void>failure(new IOException("boom"))
                .callSubscribeOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertFailure(IOException.class, "boom");
    }

}