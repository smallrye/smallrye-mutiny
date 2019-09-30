package io.smallrye.reactive.unimulti.operators;

import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.infrastructure.Infrastructure;
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
        Uni.createFrom().deferredItem(() -> 1)
                .subscribeOn(ForkJoinPool.commonPool())
                .subscribe().withSubscriber(ts);
        ts.await().assertItem(1);
        assertThat(ts.getOnSubscribeThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    public void testWithWithImmediateValue() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        Uni.createFrom().item(1)
                .subscribeOn(ForkJoinPool.commonPool())
                .subscribe().withSubscriber(ts);

        ts.await().assertItem(1);
        assertThat(ts.getOnSubscribeThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    public void testWithTimeout() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        Uni.createFrom().deferredItem(() -> {
            try {
                TimeUnit.SECONDS.sleep(1L);
            } catch (InterruptedException e) {
                // ignored
            }
            return 0;
        })
                .onNoItem().after(Duration.ofMillis(100)).recoverWithUni(Uni.createFrom().deferredItem(() -> 1))
                .subscribeOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(ts);

        ts.await().assertItem(1);
    }

    @Test
    public void callableEvaluatedTheRightTime() {
        AtomicInteger count = new AtomicInteger();

        Uni<Integer> uni = Uni.createFrom().deferredItem(count::incrementAndGet)
                .subscribeOn(ForkJoinPool.commonPool());

        assertThat(count).hasValue(0);
        uni.subscribe().withSubscriber(UniAssertSubscriber.create()).await();
        assertThat(count).hasValue(1);
    }

    @Test
    public void testWithFailure() {
        Uni.createFrom().<Void>failure(new IOException("boom"))
                .subscribeOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertFailure(IOException.class, "boom");
    }

}