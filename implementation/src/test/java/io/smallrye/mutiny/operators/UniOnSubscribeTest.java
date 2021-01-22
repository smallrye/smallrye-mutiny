package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.UniOnSubscribeCall;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnSubscribeTest {

    @Test
    public void testInvoke() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<UniSubscription> reference = new AtomicReference<>();
        Uni<Integer> uni = Uni.createFrom().item(1)
                .onSubscribe().invoke(s -> {
                    reference.set(s);
                    count.incrementAndGet();
                });

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        assertThat(count).hasValue(0);
        assertThat(reference).hasValue(null);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(1);
        assertThat(reference).doesNotHaveValue(null);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(2);
        assertThat(reference).doesNotHaveValue(null);
    }

    @Test
    public void testInvokeRunnable() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().item(1)
                .onSubscribe().invoke(count::incrementAndGet);

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        assertThat(count).hasValue(0);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(1);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(2);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecatedOnSubscribed() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<UniSubscription> reference = new AtomicReference<>();
        Uni<Integer> uni = Uni.createFrom().item(1)
                .on().subscribed(s -> {
                    reference.set(s);
                    count.incrementAndGet();
                });

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        assertThat(count).hasValue(0);
        assertThat(reference).hasValue(null);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(1);
        assertThat(reference).doesNotHaveValue(null);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(2);
        assertThat(reference).doesNotHaveValue(null);
    }

    @Test
    public void testCall() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<UniSubscription> reference = new AtomicReference<>();
        AtomicReference<UniSubscription> sub = new AtomicReference<>();
        Uni<Integer> uni = Uni.createFrom().item(1)
                .onSubscribe().call(s -> {
                    reference.set(s);
                    count.incrementAndGet();
                    return Uni.createFrom().nullItem()
                            .onSubscribe().invoke(sub::set);
                });

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        assertThat(count).hasValue(0);
        assertThat(reference).hasValue(null);
        assertThat(sub).hasValue(null);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(1);
        assertThat(reference).doesNotHaveValue(null);
        assertThat(sub).doesNotHaveValue(null);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(2);
        assertThat(reference).doesNotHaveValue(null);
        assertThat(sub).doesNotHaveValue(null);

    }

    @Test
    public void testCallWithSupplier() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<UniSubscription> sub = new AtomicReference<>();
        Uni<Integer> uni = Uni.createFrom().item(1)
                .onSubscribe().call(() -> {
                    count.incrementAndGet();
                    return Uni.createFrom().nullItem()
                            .onSubscribe().invoke(sub::set);
                });

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        assertThat(count).hasValue(0);
        assertThat(sub).hasValue(null);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(1);
        assertThat(sub).doesNotHaveValue(null);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(2);
        assertThat(sub).doesNotHaveValue(null);

    }

    @Test
    public void testInvokeUniDeprecated() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<UniSubscription> reference = new AtomicReference<>();
        AtomicReference<UniSubscription> sub = new AtomicReference<>();
        Uni<Integer> uni = Uni.createFrom().item(1)
                .onSubscribe().invokeUni(s -> {
                    reference.set(s);
                    count.incrementAndGet();
                    return Uni.createFrom().nullItem()
                            .onSubscribe().invoke(sub::set);
                });

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        assertThat(count).hasValue(0);
        assertThat(reference).hasValue(null);
        assertThat(sub).hasValue(null);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(1);
        assertThat(reference).doesNotHaveValue(null);
        assertThat(sub).doesNotHaveValue(null);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(2);
        assertThat(reference).doesNotHaveValue(null);
        assertThat(sub).doesNotHaveValue(null);

    }

    @Test
    public void testDelayedCallAfterFailure() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<UniSubscription> reference = new AtomicReference<>();
        AtomicReference<UniSubscription> sub = new AtomicReference<>();
        Uni<Object> uni = Uni.createFrom().failure(new IOException("boom"))
                .onSubscribe().call(s -> {
                    reference.set(s);
                    count.incrementAndGet();
                    return Uni.createFrom().emitter(e -> {
                        new Thread(() -> {
                            await().during(100, TimeUnit.MILLISECONDS);
                            e.complete("yo");
                        }).start();
                    })
                            .onSubscribe().invoke(sub::set);
                });

        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();

        assertThat(count).hasValue(0);
        assertThat(reference).hasValue(null);
        assertThat(sub).hasValue(null);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(1);
        assertThat(reference).doesNotHaveValue(null);
        assertThat(sub).doesNotHaveValue(null);

        uni.subscribe().withSubscriber(subscriber);

        assertThat(count).hasValue(2);
        assertThat(reference).doesNotHaveValue(null);
        assertThat(sub).doesNotHaveValue(null);

    }

    @Test
    public void testInvokeThrowingException() {
        Uni<Integer> uni = Uni.createFrom().item(1)
                .onSubscribe().invoke(s -> {
                    throw new IllegalStateException("boom");
                });

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        uni.subscribe().withSubscriber(subscriber)
                .assertFailedWith(IllegalStateException.class, "boom");

    }

    @Test
    public void testCallThrowingException() {
        Uni<Integer> uni = Uni.createFrom().item(1)
                .onSubscribe().call(s -> {
                    throw new IllegalStateException("boom");
                });

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        uni.subscribe().withSubscriber(subscriber)
                .assertFailedWith(IllegalStateException.class, "boom");

    }

    @Test
    public void testCallProvidingFailure() {
        Uni<Integer> uni = Uni.createFrom().item(1)
                .onSubscribe().call(s -> Uni.createFrom().failure(new IOException("boom")));

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        uni.subscribe().withSubscriber(subscriber)
                .assertFailedWith(IOException.class, "boom");

    }

    @Test
    public void testCallReturningNullUni() {
        Uni<Integer> uni = Uni.createFrom().item(1)
                .onSubscribe().call(s -> null);

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        uni.subscribe().withSubscriber(subscriber)
                .assertFailedWith(NullPointerException.class, "`null`");

    }

    @Test
    public void testThatInvokeConsumerCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(1)
                .onSubscribe().invoke((Consumer<? super UniSubscription>) null));
    }

    @Test
    public void testThatCallFunctionCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(1)
                .onSubscribe().call((Function<? super UniSubscription, Uni<?>>) null));
    }

    @Test
    public void testThatCallUpstreamCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new UniOnSubscribeCall<>(null, s -> Uni.createFrom().nullItem()));

    }

    @Test
    public void testThatSubscriptionIsNotPassedDownstreamUntilInvokeCallbackCompletes() {
        CountDownLatch latch = new CountDownLatch(1);
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(1)
                .onSubscribe().invoke(s -> {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertNotSubscribed();
        latch.countDown();
        subscriber.await()
                .assertSubscribed()
                .assertCompleted().assertItem(1);
    }

    @Test
    public void testThatSubscriptionIsNotPassedDownstreamUntilProducedUniCompletes() {
        AtomicReference<UniEmitter<? super Integer>> emitter = new AtomicReference<>();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> 1)
                .onSubscribe()
                .call(s -> Uni.createFrom().emitter((Consumer<UniEmitter<? super Integer>>) emitter::set))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertNotSubscribed();

        await().until(() -> emitter.get() != null);
        emitter.get().complete(12345);

        subscriber.await()
                .assertSubscribed()
                .assertCompleted().assertItem(1);

    }

    @Test
    public void testThatSubscriptionIsNotPassedDownstreamUntilProducedUniCompletesWithDifferentThread() {
        AtomicReference<UniEmitter<? super Integer>> emitter = new AtomicReference<>();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(() -> 1)
                .onSubscribe()
                .call(s -> Uni.createFrom().emitter((Consumer<UniEmitter<? super Integer>>) emitter::set))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertNotSubscribed();

        await().until(() -> emitter.get() != null);
        emitter.get().complete(12345);

        subscriber.await()
                .assertSubscribed()
                .assertCompleted().assertItem(1);

    }
}
