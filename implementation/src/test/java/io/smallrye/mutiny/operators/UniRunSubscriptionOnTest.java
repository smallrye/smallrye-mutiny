package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.UniSubscriber;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class UniRunSubscriptionOnTest {

    @Test
    public void testRunSubscriptionOnWithSupplier() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().item(() -> 1)
                .runSubscriptionOn(ForkJoinPool.commonPool())
                .subscribe().withSubscriber(subscriber);
        subscriber.awaitItem().assertItem(1);
        assertThat(subscriber.getOnSubscribeThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    public void testWithWithImmediateValue() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        Uni.createFrom().item(1)
                .runSubscriptionOn(ForkJoinPool.commonPool())
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitItem().assertItem(1);
        assertThat(subscriber.getOnSubscribeThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    public void testWithTimeout() {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

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
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitItem().assertItem(1);

        executorService.shutdownNow();
    }

    @Test
    public void callableEvaluatedTheRightTime() {
        AtomicInteger count = new AtomicInteger();

        Uni<Integer> uni = Uni.createFrom().item(count::incrementAndGet)
                .runSubscriptionOn(ForkJoinPool.commonPool());

        assertThat(count).hasValue(0);
        uni.subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem();
        assertThat(count).hasValue(1);
    }

    @Test
    public void testWithFailure() {
        Uni.createFrom().<Void> failure(new IOException("boom"))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testImmediateCancellation() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().<Integer> emitter(e -> {
            // Do nothing
        })
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        subscriber.assertNotTerminated();
    }

    @Test
    public void testCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().<Integer> emitter(e -> called.set(true))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        await().untilAsserted(subscriber::assertSubscribed);
        subscriber.assertSubscribed();
        assertThat(called).isTrue();
    }

    @Test
    public void testRejectedTask() {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        pool.shutdown();
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().<Integer> emitter(e -> called.set(true))
                .runSubscriptionOn(pool)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(called).isFalse();
        subscriber.awaitFailure()
                .assertFailedWith(RejectedExecutionException.class, "");
    }

    @Test
    public void testSubscriptionFailing() {
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> subscriber = new AbstractUni<Integer>() {

            @Override
            public void subscribe(UniSubscriber<? super Integer> subscriber) {
                throw new IllegalArgumentException("boom");
            }
        }
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertThat(called).isFalse();
        subscriber
                .awaitFailure()
                .assertFailedWith(IllegalArgumentException.class, "boom");
    }

}
