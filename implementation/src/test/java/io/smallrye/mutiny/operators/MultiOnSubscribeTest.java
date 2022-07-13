package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiOnSubscribeCall;
import io.smallrye.mutiny.operators.multi.MultiOnSubscribeInvokeOp;
import io.smallrye.mutiny.subscription.UniEmitter;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiOnSubscribeTest {

    @Test
    public void testInvoke() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<Subscription> reference = new AtomicReference<>();
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscription().invoke(s -> {
                    reference.set(s);
                    count.incrementAndGet();
                });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        assertThat(count).hasValue(0);
        assertThat(reference).hasValue(null);

        multi.subscribe().withSubscriber(subscriber).assertCompleted().assertItems(1, 2, 3);

        assertThat(count).hasValue(1);
        assertThat(reference).doesNotHaveValue(null);

        AssertSubscriber<Integer> subscriber2 = AssertSubscriber.create(10);
        multi.subscribe().withSubscriber(subscriber2).assertCompleted().assertItems(1, 2, 3);

        assertThat(count).hasValue(2);
        assertThat(reference).doesNotHaveValue(null);
    }

    @Test
    public void testDeprecatedOnSubscribed() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<Subscription> reference = new AtomicReference<>();
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscription().invoke(s -> {
                    reference.set(s);
                    count.incrementAndGet();
                });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        assertThat(count).hasValue(0);
        assertThat(reference).hasValue(null);

        multi.subscribe().withSubscriber(subscriber).assertCompleted().assertItems(1, 2, 3);

        assertThat(count).hasValue(1);
        assertThat(reference).doesNotHaveValue(null);

        AssertSubscriber<Integer> subscriber2 = AssertSubscriber.create(10);
        multi.subscribe().withSubscriber(subscriber2).assertCompleted().assertItems(1, 2, 3);

        assertThat(count).hasValue(2);
        assertThat(reference).doesNotHaveValue(null);
    }

    @Test
    public void testCall() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<Subscription> reference = new AtomicReference<>();
        AtomicReference<Subscription> sub = new AtomicReference<>();
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscription().call(s -> {
                    reference.set(s);
                    count.incrementAndGet();
                    return Uni.createFrom().nullItem()
                            .onSubscription().invoke(sub::set);
                });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        assertThat(count).hasValue(0);
        assertThat(reference).hasValue(null);

        multi.subscribe().withSubscriber(subscriber).assertCompleted().assertItems(1, 2, 3);

        assertThat(count).hasValue(1);
        assertThat(reference).doesNotHaveValue(null);

        AssertSubscriber<Integer> subscriber2 = AssertSubscriber.create(10);
        multi.subscribe().withSubscriber(subscriber2).assertCompleted().assertItems(1, 2, 3);

        assertThat(count).hasValue(2);
        assertThat(reference).doesNotHaveValue(null);
    }

    @Test
    public void testCallWithSupplier() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<Subscription> sub = new AtomicReference<>();
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscription().call(() -> {
                    count.incrementAndGet();
                    return Uni.createFrom().nullItem()
                            .onSubscription().invoke(sub::set);
                });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        assertThat(count).hasValue(0);

        multi.subscribe().withSubscriber(subscriber).assertCompleted().assertItems(1, 2, 3);

        assertThat(count).hasValue(1);

        AssertSubscriber<Integer> subscriber2 = AssertSubscriber.create(10);
        multi.subscribe().withSubscriber(subscriber2).assertCompleted().assertItems(1, 2, 3);

        assertThat(count).hasValue(2);
    }

    @Test
    public void testInvokeThrowingException() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscription().invoke(s -> {
                    throw new IllegalStateException("boom");
                });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        multi.subscribe().withSubscriber(subscriber)
                .assertFailedWith(IllegalStateException.class, "boom");

    }

    @Test
    public void testCallThrowingException() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscription().call(s -> {
                    throw new IllegalStateException("boom");
                });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        multi.subscribe().withSubscriber(subscriber)
                .assertFailedWith(IllegalStateException.class, "boom");

    }

    @Test
    public void testCallProvidingFailure() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscription().call(s -> Uni.createFrom().failure(new IOException("boom")));

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        multi.subscribe().withSubscriber(subscriber)
                .assertFailedWith(IOException.class, "boom");

    }

    @Test
    public void testCallReturningNullUni() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onSubscription().call(s -> null);

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        multi.subscribe().withSubscriber(subscriber)
                .assertFailedWith(NullPointerException.class, "`null`");
    }

    @Test
    public void testThatInvokeConsumerCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().items(1, 2, 3)
                .onSubscription().invoke((Consumer<? super Subscription>) null));
    }

    @Test
    public void testThatCallFunctionCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().items(1, 2, 3)
                .onSubscription().call((Function<? super Subscription, Uni<?>>) null));
    }

    @Test
    public void testThatInvokeUpstreamCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new MultiOnSubscribeInvokeOp<>(null, s -> {
        }));
    }

    @Test
    public void testThatCallUpstreamCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiOnSubscribeCall<>(null, s -> Uni.createFrom().nullItem()));
    }

    @Test
    public void testThatSubscriptionIsNotPassedDownstreamUntilInvokeCallbackCompletes() {
        CountDownLatch latch = new CountDownLatch(1);
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3)
                .onSubscription().invoke(s -> {
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
        subscriber
                .awaitSubscription()
                .awaitCompletion()
                .assertItems(1, 2, 3);
    }

    @Test
    public void testThatSubscriptionIsNotPassedDownstreamUntilProducedUniCompletes() {
        AtomicReference<UniEmitter<? super Integer>> emitter = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3)
                .onSubscription()
                .call(s -> Uni.createFrom().emitter((Consumer<UniEmitter<? super Integer>>) emitter::set))
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.assertNotSubscribed();

        await().until(() -> emitter.get() != null);
        emitter.get().complete(12345);

        subscriber
                .awaitSubscription()
                .awaitCompletion()
                .assertItems(1, 2, 3);

    }

    @Test
    public void testThatSubscriptionIsNotPassedDownstreamUntilProducedUniCompletesWithDifferentThread() {
        AtomicReference<UniEmitter<? super Integer>> emitter = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3)
                .onSubscription()
                .call(s -> Uni.createFrom().emitter((Consumer<UniEmitter<? super Integer>>) emitter::set))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.assertNotSubscribed();

        await().until(() -> emitter.get() != null);
        emitter.get().complete(12345);

        subscriber
                .awaitSubscription()
                .awaitCompletion()
                .assertItems(1, 2, 3);

    }

    @Test
    public void testThatRunSubscriptionOnEmitRequestOnSubscribe() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(AssertSubscriber.create(2));

        subscriber
                .request(1)
                .awaitCompletion()
                .assertItems(1, 2, 3);
    }

    @Test
    public void testRunSubscriptionOnShutdownExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.shutdownNow();

        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3)
                .runSubscriptionOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(2));

        subscriber.assertFailedWith(RejectedExecutionException.class, "");
    }

    @Test
    public void testRunSubscriptionOnShutdownExecutorRequests() {
        ExecutorService executor = Executors.newFixedThreadPool(1);

        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3)
                .runSubscriptionOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(0));

        await().untilAsserted(subscriber::assertSubscribed);

        subscriber.assertHasNotReceivedAnyItem();
        executor.shutdownNow();
        subscriber.request(2);

        subscriber.assertFailedWith(RejectedExecutionException.class, "");
    }

}
