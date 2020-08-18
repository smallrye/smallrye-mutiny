package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

public class UniSerializedSubscriberTest {

    @Test
    public void testNormal() {
        AtomicReference<UniEmitter<? super Integer>> reference = new AtomicReference<>();
        AbstractUni<? super Integer> uni = (AbstractUni<? super Integer>) Uni.createFrom().<Integer> emitter(
                reference::set);

        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();
        UniSerializedSubscriber.subscribe(uni, subscriber);

        subscriber.assertSubscribed();

        reference.get().complete(1);
        reference.get().complete(2);
        reference.get().fail(new IOException("boom"));

        subscriber
                .assertCompletedSuccessfully()
                .assertNoFailure()
                .assertItem(1);
    }

    @Test
    public void testFailure() {
        AtomicReference<UniEmitter<? super Integer>> reference = new AtomicReference<>();
        AbstractUni<? super Integer> uni = (AbstractUni<? super Integer>) Uni.createFrom().<Integer> emitter(
                reference::set);

        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();
        UniSerializedSubscriber.subscribe(uni, subscriber);

        subscriber.assertSubscribed();

        reference.get().fail(new IOException("boom"));
        subscriber
                .assertNoResult()
                .assertCompletedWithFailure()
                .assertFailure(IOException.class, "boom");
    }

    @Test
    public void testNormalWithNullItem() {
        AtomicReference<UniEmitter<? super String>> reference = new AtomicReference<>();
        AbstractUni<? super String> uni = (AbstractUni<? super String>) Uni.createFrom().<String> emitter(
                reference::set);

        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();
        UniSerializedSubscriber.subscribe(uni, subscriber);

        subscriber.assertSubscribed();

        reference.get().complete(null);
        reference.get().complete("hello");
        reference.get().fail(new IOException("boom"));

        subscriber
                .assertCompletedSuccessfully()
                .assertNoFailure()
                .assertItem(null);
    }

    @Test
    public void testRogueUpstreamSendingFailureBeforeSubscription() {
        AbstractUni<Integer> rogue = new AbstractUni<Integer>() {
            @Override
            protected void subscribing(UniSerializedSubscriber<? super Integer> subscriber) {
                subscriber.onFailure(new IOException("boom"));
                subscriber.onSubscribe(() -> {
                });
            }
        };

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        UniSerializedSubscriber.subscribe(rogue, subscriber);

        subscriber
                .assertSubscribed()
                .assertFailure(IllegalStateException.class, "Invalid");

    }

    @Test
    public void testRogueUpstreamSendingItemBeforeSubscription() {
        AbstractUni<Integer> rogue = new AbstractUni<Integer>() {
            @Override
            protected void subscribing(UniSerializedSubscriber<? super Integer> subscriber) {
                subscriber.onItem(1);
                subscriber.onSubscribe(() -> {
                });
            }
        };

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        UniSerializedSubscriber.subscribe(rogue, subscriber);

        subscriber
                .assertSubscribed()
                .assertFailure(IllegalStateException.class, "Invalid");

    }

    @Test
    public void testInvalidStateWhenOnSubscribeIsCalled() {
        AbstractUni<Integer> rogue = new AbstractUni<Integer>() {
            @Override
            protected void subscribing(UniSerializedSubscriber<? super Integer> subscriber) {
                // Do nothing
            }
        };

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        UniSerializedSubscriber<Integer> serialized = new UniSerializedSubscriber<>(rogue, subscriber);

        serialized.onSubscribe(() -> {
        });
        subscriber
                .assertSubscribed()
                .assertFailure(IllegalStateException.class, "Invalid transition");

    }

    @Test
    public void testRogueUpstreamSendingMultipleItems() {
        AbstractUni<Integer> rogue = new AbstractUni<Integer>() {
            @Override
            protected void subscribing(UniSerializedSubscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(() -> {
                });
                subscriber.onItem(1);
                subscriber.onItem(2);
            }
        };

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        UniSerializedSubscriber.subscribe(rogue, subscriber);

        subscriber
                .assertSubscribed()
                .assertItem(1);

    }

    @Test(invocationCount = 100)
    public void testRaceBetweenItemAndFailure() {
        AtomicReference<UniEmitter<? super Integer>> reference = new AtomicReference<>();
        Uni<Integer> uni = Uni.createFrom().<Integer> emitter(reference::set);

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        uni.subscribe().withSubscriber(subscriber);

        subscriber.assertSubscribed();

        CountDownLatch start = new CountDownLatch(2);

        Runnable runnable1 = () -> {
            start.countDown();
            await(start);
            reference.get().complete(1);
        };

        Runnable runnable2 = () -> {
            start.countDown();
            await(start);
            reference.get().fail(new IOException("boom"));
        };

        List<Runnable> runnables = Arrays.asList(runnable1, runnable2);
        Collections.shuffle(runnables);

        runnables.forEach(r -> new Thread(r).start());

        subscriber.await();

        if (subscriber.getFailure() != null) {
            subscriber.assertCompletedWithFailure()
                    .assertFailure(IOException.class, "boom");
        } else {
            subscriber.assertCompletedSuccessfully()
                    .assertItem(1);
        }
    }

    @Test(invocationCount = 100)
    public void testRaceBetweenMultipleItems() {
        AtomicReference<UniEmitter<? super Integer>> reference = new AtomicReference<>();
        Uni<Integer> uni = Uni.createFrom().<Integer> emitter(reference::set);

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        uni.subscribe().withSubscriber(subscriber);

        subscriber.assertSubscribed();

        CountDownLatch start = new CountDownLatch(3);

        Runnable runnable1 = () -> {
            start.countDown();
            await(start);
            reference.get().complete(1);
        };

        Runnable runnable2 = () -> {
            start.countDown();
            await(start);
            reference.get().complete(2);
        };

        Runnable runnable3 = () -> {
            start.countDown();
            await(start);
            reference.get().complete(3);
        };

        List<Runnable> runnables = Arrays.asList(runnable1, runnable2, runnable3);
        Collections.shuffle(runnables);

        runnables.forEach(r -> new Thread(r).start());

        subscriber.await();
        subscriber.assertCompletedSuccessfully();
        assertThat(subscriber.getItem()).isBetween(1, 3);
    }

    @Test(invocationCount = 100)
    public void testRaceBetweenItemAndCancellation() {
        AtomicReference<UniEmitter<? super Integer>> reference = new AtomicReference<>();
        AtomicBoolean cancelled = new AtomicBoolean();
        Uni<Integer> uni = Uni.createFrom().<Integer> emitter(reference::set)
                .onCancellation().invoke(() -> cancelled.set(true));

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        uni.subscribe().withSubscriber(subscriber);

        subscriber.assertSubscribed();

        CountDownLatch start = new CountDownLatch(2);
        CountDownLatch done = new CountDownLatch(2);

        Runnable runnable1 = () -> {
            try {
                start.countDown();
                await(start);
                reference.get().complete(1);
                done.countDown();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };

        Runnable runnable2 = () -> {
            try {
                start.countDown();
                await(start);
                subscriber.cancel();
                done.countDown();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };

        List<Runnable> runnables = Arrays.asList(runnable1, runnable2);
        Collections.shuffle(runnables);

        runnables.forEach(r -> new Thread(r).start());

        await(done);

        if (cancelled.get()) {
            subscriber.assertNotCompleted();
        } else {
            subscriber
                    .await()
                    .assertCompletedSuccessfully()
                    .assertItem(1);
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
