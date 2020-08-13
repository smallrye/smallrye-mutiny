package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOnSubscribeInvokeOp;
import io.smallrye.mutiny.operators.multi.MultiOnSubscribeInvokeUniOp;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiOnSubscribeTest {

    @Test
    public void testInvoke() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<Subscription> reference = new AtomicReference<>();
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscribe().invoke(s -> {
                    reference.set(s);
                    count.incrementAndGet();
                });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        assertThat(count).hasValue(0);
        assertThat(reference).hasValue(null);

        multi.subscribe().withSubscriber(subscriber).assertCompletedSuccessfully().assertReceived(1, 2, 3);

        assertThat(count).hasValue(1);
        assertThat(reference).doesNotHaveValue(null);

        AssertSubscriber<Integer> subscriber2 = AssertSubscriber.create(10);
        multi.subscribe().withSubscriber(subscriber2).assertCompletedSuccessfully().assertReceived(1, 2, 3);

        assertThat(count).hasValue(2);
        assertThat(reference).doesNotHaveValue(null);
    }

    @Test
    public void testDeprecatedOnSubscribed() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<Subscription> reference = new AtomicReference<>();
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscribe().invoke(s -> {
                    reference.set(s);
                    count.incrementAndGet();
                });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        assertThat(count).hasValue(0);
        assertThat(reference).hasValue(null);

        multi.subscribe().withSubscriber(subscriber).assertCompletedSuccessfully().assertReceived(1, 2, 3);

        assertThat(count).hasValue(1);
        assertThat(reference).doesNotHaveValue(null);

        AssertSubscriber<Integer> subscriber2 = AssertSubscriber.create(10);
        multi.subscribe().withSubscriber(subscriber2).assertCompletedSuccessfully().assertReceived(1, 2, 3);

        assertThat(count).hasValue(2);
        assertThat(reference).doesNotHaveValue(null);
    }

    @Test
    public void testInvokeUni() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<Subscription> reference = new AtomicReference<>();
        AtomicReference<Subscription> sub = new AtomicReference<>();
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscribe().invokeUni(s -> {
                    reference.set(s);
                    count.incrementAndGet();
                    return Uni.createFrom().nullItem()
                            .onSubscribe().invoke(sub::set);
                });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        assertThat(count).hasValue(0);
        assertThat(reference).hasValue(null);

        multi.subscribe().withSubscriber(subscriber).assertCompletedSuccessfully().assertReceived(1, 2, 3);

        assertThat(count).hasValue(1);
        assertThat(reference).doesNotHaveValue(null);

        AssertSubscriber<Integer> subscriber2 = AssertSubscriber.create(10);
        multi.subscribe().withSubscriber(subscriber2).assertCompletedSuccessfully().assertReceived(1, 2, 3);

        assertThat(count).hasValue(2);
        assertThat(reference).doesNotHaveValue(null);
    }

    @Test
    public void testInvokeThrowingException() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscribe().invoke(s -> {
                    throw new IllegalStateException("boom");
                });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        multi.subscribe().withSubscriber(subscriber)
                .assertHasFailedWith(IllegalStateException.class, "boom");

    }

    @Test
    public void testInvokeUniThrowingException() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscribe().invokeUni(s -> {
                    throw new IllegalStateException("boom");
                });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        multi.subscribe().withSubscriber(subscriber)
                .assertHasFailedWith(IllegalStateException.class, "boom");

    }

    @Test
    public void testInvokeUniProvidingFailure() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscribe().invokeUni(s -> Uni.createFrom().failure(new IOException("boom")));

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        multi.subscribe().withSubscriber(subscriber)
                .assertHasFailedWith(IOException.class, "boom");

    }

    @Test
    public void testInvokeUniReturningNullUni() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscribe().invokeUni(s -> null);

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        multi.subscribe().withSubscriber(subscriber)
                .assertHasFailedWith(NullPointerException.class, "`null`");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatInvokeConsumerCannotBeNull() {
        Multi.createFrom().items(1, 2, 3)
                .onSubscribe().invoke(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatInvokeUniFunctionCannotBeNull() {
        Multi.createFrom().items(1, 2, 3)
                .onSubscribe().invokeUni(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatInvokeUpstreamCannotBeNull() {
        new MultiOnSubscribeInvokeOp<>(null, s -> {
        });
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatInvokeUniUpstreamCannotBeNull() {
        new MultiOnSubscribeInvokeUniOp<>(null, s -> Uni.createFrom().nullItem());
    }

    @Test
    public void testThatSubscriptionIsNotPassedDownstreamUntilInvokeCallbackCompletes() {
        CountDownLatch latch = new CountDownLatch(1);
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3)
                .onSubscribe().invoke(s -> {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.assertNotSubscribed();
        latch.countDown();
        subscriber.await()
                .assertSubscribed()
                .assertCompletedSuccessfully().assertReceived(1, 2, 3);
    }

    @Test
    public void testThatSubscriptionIsNotPassedDownstreamUntilProducedUniCompletes() {
        AtomicReference<UniEmitter<? super Integer>> emitter = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3)
                .onSubscribe().invokeUni(s -> Uni.createFrom().emitter((Consumer<UniEmitter<? super Integer>>) emitter::set))
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.assertNotSubscribed();

        await().until(() -> emitter.get() != null);
        emitter.get().complete(12345);

        subscriber.await()
                .assertSubscribed()
                .assertCompletedSuccessfully().assertReceived(1, 2, 3);

    }

    @Test
    public void testThatSubscriptionIsNotPassedDownstreamUntilProducedUniCompletesWithDifferentThread() {
        AtomicReference<UniEmitter<? super Integer>> emitter = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3)
                .onSubscribe().invokeUni(s -> Uni.createFrom().emitter((Consumer<UniEmitter<? super Integer>>) emitter::set))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.assertNotSubscribed();

        await().until(() -> emitter.get() != null);
        emitter.get().complete(12345);

        subscriber.await()
                .assertSubscribed()
                .assertCompletedSuccessfully().assertReceived(1, 2, 3);

    }

    @Test
    public void testThatRunOnSubscriptionEmitRequestOnSubscribe() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(AssertSubscriber.create(2));

        subscriber
                .request(1)
                .await()
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();
    }
}
