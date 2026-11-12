package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiIfNoItemTest {

    @Test
    public void testResultWhenTimeoutIsNotReached() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create(10);

        Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .select().first(4)
                .ifNoItem().after(Duration.ofMillis(20)).recoverWithCompletion()
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitCompletion().assertItems(0L, 1L, 2L, 3L);
    }

    @Test
    public void testTimeoutBeforeFirstItem() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create(10);

        Multi.createFrom().ticks().startingAfter(Duration.ofMillis(20)).every(Duration.ofMillis(20))
                .ifNoItem().after(Duration.ofMillis(10)).fail()
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitFailure()
                .assertHasNotReceivedAnyItem()
                .assertFailedWith(TimeoutException.class);
    }

    @Test
    public void testTimeoutAfterFirstItem() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create(10);

        Multi.createFrom().ticks().every(Duration.ofMillis(20))
                .ifNoItem().after(Duration.ofMillis(10)).fail()
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitFailure()
                .assertItems(0L)
                .assertFailedWith(TimeoutException.class);
    }

    @Test
    public void testTimeoutOnFailure() {
        AssertSubscriber<Long> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Long> emitter(e -> new Thread(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
            e.fail(new TestException("boom"));
        }).start())
                .ifNoItem().after(Duration.ofMillis(1)).fail()
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitFailure()
                .assertFailedWith(TimeoutException.class);
    }

    @Test
    public void testFailureBeforeTimeout() {
        AtomicBoolean subscribed = new AtomicBoolean(false);
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().<Integer> emitter(e -> new Thread(() -> {
            await().untilTrue(subscribed);
            e.fail(new TestException("boom"));
        }).start())
                .ifNoItem().after(Duration.ofMillis(10000)).fail()
                .subscribe().withSubscriber(subscriber);

        subscribed.set(true);
        subscriber.awaitFailure()
                .assertFailedWith(TestException.class);
    }

    @Test
    public void testRecoverWithCompletion() {
        AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithCompletion()
                .subscribe().withSubscriber(AssertSubscriber.create());
        subscriber.awaitCompletion();
    }

    @Test
    public void testRecoverWithSwitchToMulti() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithMulti(Multi.createFrom().item(15))
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        subscriber.awaitCompletion().assertItems(15);
    }

    @Test
    public void testRecoverWithSwitchToMultiSupplier() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).recoverWithMulti(() -> Multi.createFrom().item(15))
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        subscriber.awaitCompletion().assertItems(15);
    }

    @Test
    public void testFailingWithAnotherException() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> nothing()
                .ifNoItem().after(Duration.ofMillis(10)).failWith(new IOException("boom"))
                .subscribe().withSubscriber(AssertSubscriber.create());
        subscriber.awaitFailure().assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDurationValidity() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createFrom().item(1).ifNoItem().after(null))
                .withMessageContaining("timeout");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createFrom().item(1).ifNoItem().after(Duration.ofMillis(0)))
                .withMessageContaining("timeout");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> Multi.createFrom().item(1).ifNoItem().after(Duration.ofMillis(-1)))
                .withMessageContaining("timeout");
    }

    @Test
    public void testFailingOnTimeoutWithShutdownExecutor() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.shutdown();
        AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofMillis(10)).on(executor).fail()
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber
                .awaitFailure()
                .assertFailedWith(RejectedExecutionException.class, "");
    }

    @Test
    public void testFailingOnItemWithShutdownExecutor() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        AssertSubscriber<Object> subscriber = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .ifNoItem().after(Duration.ofMillis(20)).on(executor).fail()
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        executor.shutdown();

        subscriber
                .awaitFailure()
                .assertFailedWith(RejectedExecutionException.class, "");
    }

    @Test
    public void testFailingOnTimeoutWithImmediateCancellation() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofMillis(10)).on(executor).fail()
                .subscribe().withSubscriber(new AssertSubscriber<>(1, true));

        subscriber.assertNotTerminated()
                .assertSubscribed();
    }

    @Test
    public void testFailingOnTimeoutWithCancellation() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofMillis(1000)).on(executor).fail()
                .subscribe().withSubscriber(new AssertSubscriber<>(1, false));

        subscriber.cancel();

        subscriber.assertNotTerminated()
                .assertSubscribed();
    }

    @Test
    public void testTimeoutWithCustomSupplierFailing() {
        AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofMillis(1)).failWith(() -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.awaitFailure()
                .assertFailedWith(IllegalArgumentException.class);
    }

    @Test
    public void testTimeoutWithCustomSupplierReturningNull() {
        AssertSubscriber<Object> subscriber = Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofMillis(1)).failWith(() -> null)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.awaitFailure()
                .assertFailedWith(NullPointerException.class, ParameterValidation.SUPPLIER_PRODUCED_NULL);
    }

    @Test
    public void testRejectedSchedulingAtSubscriptionSignalsOnSubscribeFirst() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.shutdown();
        List<String> events = new ArrayList<>();

        Multi.createFrom().nothing()
                .ifNoItem().after(Duration.ofSeconds(10)).on(executor).fail()
                .subscribe().withSubscriber(new MultiSubscriber<Object>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        events.add("onSubscribe");
                    }

                    @Override
                    public void onItem(Object item) {
                        events.add("onItem");
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                        events.add("onFailure:" + failure.getClass().getSimpleName());
                    }

                    @Override
                    public void onCompletion() {
                        events.add("onCompletion");
                    }
                });

        assertThat(events).containsExactly("onSubscribe", "onFailure:RejectedExecutionException");
    }

    @Test
    public void testRejectedReschedulingOnItemCancelsUpstream() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            AtomicBoolean upstreamCancelled = new AtomicBoolean();
            AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
            AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(emitter::set)
                    .onCancellation().invoke(() -> upstreamCancelled.set(true))
                    .ifNoItem().after(Duration.ofSeconds(10)).on(executor).fail()
                    .subscribe().withSubscriber(AssertSubscriber.create(10));
            executor.shutdownNow();

            emitter.get().emit(1);

            subscriber.assertFailedWith(RejectedExecutionException.class);
            assertThat(upstreamCancelled).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testTimeoutFiringBeforeUpstreamIsStoredSignalsOnSubscribeAndFailure() {
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1) {
            @Override
            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                new FutureTask<>(command, null).run();
                return super.schedule(() -> {
                }, delay, unit);
            }
        };
        List<String> events = new ArrayList<>();
        try {
            Multi.createFrom().nothing()
                    .ifNoItem().after(Duration.ofSeconds(10)).on(executor).fail()
                    .subscribe().withSubscriber(new MultiSubscriber<Object>() {
                        @Override
                        public void onSubscribe(Subscription s) {
                            events.add("onSubscribe");
                        }

                        @Override
                        public void onItem(Object item) {
                            events.add("onItem");
                        }

                        @Override
                        public void onFailure(Throwable failure) {
                            events.add("onFailure:" + failure.getClass().getSimpleName());
                        }

                        @Override
                        public void onCompletion() {
                            events.add("onCompletion");
                        }
                    });

            assertThat(events).containsExactly("onSubscribe", "onFailure:TimeoutException");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
    public void testRejectedReschedulingAfterTimeoutDoesNotSignalTwice() {
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1) {
            private final AtomicInteger calls = new AtomicInteger();

            @Override
            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                if (calls.getAndIncrement() > 0) {
                    throw new RejectedExecutionException("rejected");
                }
                ScheduledFuture<?> future = mock(ScheduledFuture.class);
                when(future.cancel(false)).thenAnswer(invocation -> {
                    command.run();
                    return true;
                });
                return future;
            }
        };
        List<String> events = new ArrayList<>();
        List<Throwable> dropped = new ArrayList<>();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(dropped::add);
        try {
            Multi.createFrom().<Integer> emitter(emitter::set)
                    .ifNoItem().after(Duration.ofSeconds(10)).on(executor).fail()
                    .subscribe().withSubscriber(new MultiSubscriber<Integer>() {
                        @Override
                        public void onSubscribe(Subscription s) {
                            events.add("onSubscribe");
                            s.request(Long.MAX_VALUE);
                        }

                        @Override
                        public void onItem(Integer item) {
                            events.add("onItem");
                        }

                        @Override
                        public void onFailure(Throwable failure) {
                            events.add("onFailure:" + failure.getClass().getSimpleName());
                        }

                        @Override
                        public void onCompletion() {
                            events.add("onCompletion");
                        }
                    });

            emitter.get().emit(1);

            assertThat(events).containsExactly("onSubscribe", "onFailure:TimeoutException");
            assertThat(dropped).filteredOn(RejectedExecutionException.class::isInstance).hasSize(1);
        } finally {
            Infrastructure.resetDroppedExceptionHandler();
            executor.shutdownNow();
        }
    }

    @RepeatedTest(50)
    void timeoutDoubleFailure() {
        AtomicInteger failureCount = new AtomicInteger();

        Multi.createFrom().<String> emitter(emitter -> {
            new Thread(() -> {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                emitter.fail(new RuntimeException("upstream"));
            }).start();
        }).ifNoItem().after(Duration.ofMillis(1)).fail()
                .subscribe().withSubscriber(new MultiSubscriber<>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onItem(String item) {
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                        failureCount.incrementAndGet();
                    }

                    @Override
                    public void onCompletion() {
                    }
                });

        await().atMost(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(failureCount.get()).isGreaterThanOrEqualTo(1));
        assertThat(failureCount.get()).isEqualTo(1);
    }
}
