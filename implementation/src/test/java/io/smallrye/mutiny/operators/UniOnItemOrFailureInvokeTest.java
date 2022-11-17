package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.Cancellable;

public class UniOnItemOrFailureInvokeTest {

    private final Uni<Integer> one = Uni.createFrom().item(1);
    private final Uni<Integer> two = Uni.createFrom().item(2);
    private final Uni<Void> none = Uni.createFrom().nullItem();
    private final Uni<Integer> failed = Uni.createFrom().failure(new IOException("boom"));

    @Test
    public void testThatCallbackMustNotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).onItemOrFailure().invoke((BiConsumer<? super Integer, Throwable>) null));
    }

    @Test
    public void testThatCallbackMustNotBeNullWithCall() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).onItemOrFailure().call((Supplier<Uni<?>>) null));
    }

    @Test
    public void testCallbackOnItem() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        AtomicBoolean invokedRunnable = new AtomicBoolean();
        AtomicBoolean invokedUni = new AtomicBoolean();
        AtomicBoolean calledSupplier = new AtomicBoolean();
        one
                .onItemOrFailure().invoke((i, f) -> {
                    assertThat(f).isNull();
                    count.incrementAndGet();
                })
                .onItemOrFailure().invoke(() -> invokedRunnable.set(true))
                .onItemOrFailure().call((i, r) -> {
                    invokedUni.set(true);
                    return Uni.createFrom().item(69);
                })
                .onItemOrFailure().call(() -> {
                    calledSupplier.set(true);
                    return Uni.createFrom().item(69);
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.assertCompleted()
                .assertItem(1);

        assertThat(count).hasValue(1);
        assertThat(invokedRunnable.get()).isTrue();
        assertThat(calledSupplier.get()).isTrue();
    }

    @Test
    public void testCallbackOnNullItem() {
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        none.onItemOrFailure().invoke((i, f) -> {
            assertThat(f).isNull();
            count.incrementAndGet();
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertCompleted()
                .assertItem(null);

        assertThat(count).hasValue(1);
    }

    @Test
    public void testCallbackOnFailure() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        failed.onItemOrFailure().invoke((i, f) -> {
            assertThat(i).isNull();
            assertThat(f).isNotNull().isInstanceOf(IOException.class).hasMessageContaining("boom");
            count.incrementAndGet();
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(IOException.class, "boom");

        assertThat(count).hasValue(1);
    }

    @Test
    public void testCallbackOnItemThrowingException() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        one.onItemOrFailure().invoke((i, f) -> {
            assertThat(f).isNull();
            count.incrementAndGet();
            throw new IllegalStateException("kaboom");
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(IllegalStateException.class, "kaboom");
        assertThat(count).hasValue(1);
    }

    @Test
    public void testCallbackOnItemThrowingExceptionWithCall() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        one.onItemOrFailure().invoke((i, f) -> {
            assertThat(f).isNull();
            count.incrementAndGet();
            throw new IllegalStateException("kaboom");
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(IllegalStateException.class, "kaboom");
        assertThat(count).hasValue(1);
    }

    @Test
    public void testCallbackOnFailureThrowingException() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        failed.onItemOrFailure().invoke((i, f) -> {
            assertThat(i).isNull();
            assertThat(f).isNotNull().isInstanceOf(IOException.class).hasMessageContaining("boom");
            count.incrementAndGet();
            throw new IllegalStateException("kaboom");
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(CompositeException.class, "kaboom");
        subscriber.assertFailedWith(CompositeException.class, "boom");

        assertThat(count).hasValue(1);
    }

    @Test
    public void testWithTwoSubscribers() {
        UniAssertSubscriber<Integer> s1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> s2 = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = one.onItemOrFailure().invoke((v, f) -> count.incrementAndGet());
        uni.subscribe().withSubscriber(s1);
        uni.subscribe().withSubscriber(s2);

        s1.assertCompleted()
                .assertItem(1);
        s2.assertCompleted()
                .assertItem(1);
    }

    @Test
    public void testThatCallbackIsCalledOnTheRightExecutorOnItem() {
        UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>();
        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            AtomicReference<String> threadName = new AtomicReference<>();
            one
                    .emitOn(executor)
                    .onItemOrFailure().invoke((i, f) -> threadName.set(Thread.currentThread().getName()))
                    .subscribe().withSubscriber(subscriber);

            assertThat(subscriber.awaitItem().getItem()).isEqualTo(1);
            assertThat(threadName).isNotNull().doesNotHaveValue("main");
            assertThat(subscriber.getOnItemThreadName()).isEqualTo(threadName.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testThatCallbackIsCalledOnTheRightExecutorOnFailure() {
        UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>();
        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            AtomicReference<String> threadName = new AtomicReference<>();
            failed
                    .emitOn(executor)
                    .onItemOrFailure().invoke((i, f) -> {
                        threadName.set(Thread.currentThread().getName());
                        assertThat(i).isNull();
                        assertThat(f).isNotNull();
                    })
                    .onFailure().recoverWithItem(1)
                    .subscribe().withSubscriber(subscriber);

            assertThat(subscriber.awaitItem().getItem()).isEqualTo(1);
            assertThat(threadName).isNotNull().doesNotHaveValue("main");
            assertThat(subscriber.getOnItemThreadName()).isEqualTo(threadName.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testCallOnItem() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicInteger reference = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();
        one.onItemOrFailure().call((i, f) -> {
            assertThat(f).isNull();
            count.incrementAndGet();
            return two.onItem().invoke(reference::set);
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertCompleted()
                .assertItem(1);
        assertThat(reference).hasValue(2);
        assertThat(count).hasValue(1);
    }

    @Test
    public void testCallNullOnItem() {
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        AtomicInteger reference = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();
        none.onItemOrFailure().call((i, f) -> {
            assertThat(f).isNull();
            assertThat(i).isNull();
            count.incrementAndGet();
            return two.onItem().invoke(reference::set);
        }).subscribe().withSubscriber(subscriber);

        subscriber.assertCompleted().assertItem(null);
        assertThat(reference).hasValue(2);
        assertThat(count).hasValue(1);
    }

    @Test
    public void testCallOnFailure() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicInteger reference = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();
        failed.onItemOrFailure().call((i, f) -> {
            assertThat(i).isNull();
            assertThat(f).isNotNull().isInstanceOf(IOException.class).hasMessageContaining("boom");
            count.incrementAndGet();
            return two.onItem().invoke(reference::set);

        }).subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(IOException.class, "boom");
        assertThat(reference).hasValue(2);
        assertThat(count).hasValue(1);
    }

    @Test
    public void testCallProducingNullOnItem() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> one
                        .onItemOrFailure().call((s, f) -> null)
                        .await().indefinitely());
    }

    @Test
    public void testCallProducingNullOnFailure() {
        assertThatExceptionOfType(CompositeException.class)
                .isThrownBy(() -> failed
                        .onItemOrFailure().call((s, f) -> null)
                        .await().indefinitely())
                .withMessageContaining("null").withMessageContaining("boom");
    }

    @Test
    public void testCallFailingOnItem() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> one
                        .onItemOrFailure().call((s, f) -> {
                            throw new IllegalStateException("boom");
                        })
                        .await().indefinitely());
    }

    @Test
    public void testCallFailingOnFailure() {
        assertThatExceptionOfType(CompositeException.class)
                .isThrownBy(() -> failed
                        .onItemOrFailure().call((s, f) -> {
                            throw new IllegalStateException("d'oh");
                        })
                        .await().indefinitely())
                .withMessageContaining("d'oh").withMessageContaining("boom");
    }

    @Test
    public void testCallProducingFailureOnItem() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> Uni.createFrom().item("hello")
                        .onItemOrFailure()
                        .call((s, f) -> Uni.createFrom().failure(new IllegalStateException("boom")))
                        .await().indefinitely())
                .withMessageContaining("boom");
    }

    @Test
    public void testCallProducingFailureOnFailure() {
        assertThatExceptionOfType(CompositeException.class)
                .isThrownBy(() -> failed
                        .onItemOrFailure()
                        .call((s, f) -> Uni.createFrom().failure(new IllegalStateException("d'oh")))
                        .await().indefinitely())
                .withMessageContaining("d'oh").withMessageContaining("boom");
    }

    @Test
    public void testCallWithCancellationBeforeEmission() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicReference<String> res = new AtomicReference<>();
        Uni<Object> emitter = Uni.createFrom().emitter(e -> e.onTermination(() -> called.set(true)));

        Cancellable cancellable = Uni.createFrom().item("hello")
                .onItemOrFailure().call((s, f) -> emitter)
                .subscribe().with(res::set);

        cancellable.cancel();
        assertThat(res).hasValue(null);
        assertThat(called).isTrue();
    }
}
