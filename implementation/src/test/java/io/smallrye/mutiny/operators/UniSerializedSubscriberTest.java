package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.subscription.UniSerializedSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
public class UniSerializedSubscriberTest {

    @AfterEach
    public void cleanup() {
        Infrastructure.resetDroppedExceptionHandler();
    }

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
                .assertCompleted()
                .assertItem(1)
                .assertSignalsReceivedInOrder();
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
                .assertFailedWith(IOException.class, "boom")
                .assertSignalsReceivedInOrder();
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
                .assertCompleted()
                .assertItem(null)
                .assertSignalsReceivedInOrder();
    }

    @Test
    public void testRogueUpstreamSendingFailureBeforeSubscription() {
        AbstractUni<Integer> rogue = new AbstractUni<Integer>() {
            @Override
            public void subscribe(UniSubscriber<? super Integer> subscriber) {
                subscriber.onFailure(new IOException("boom"));
                subscriber.onSubscribe(() -> {
                });
            }
        };

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        UniSerializedSubscriber.subscribe(rogue, subscriber);

        subscriber
                .assertSubscribed()
                .assertFailedWith(IOException.class, "boom")
                .assertSignalsReceivedInOrder();
    }

    @Test
    public void testRogueUpstreamSendingItemBeforeSubscription() {
        AbstractUni<Integer> rogue = new AbstractUni<Integer>() {
            @Override
            public void subscribe(UniSubscriber<? super Integer> subscriber) {
                subscriber.onItem(1);
                subscriber.onSubscribe(() -> {
                });
            }
        };

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        UniSerializedSubscriber.subscribe(rogue, subscriber);

        subscriber
                .assertSubscribed()
                .assertFailedWith(IllegalStateException.class, "Invalid")
                .assertSignalsReceivedInOrder();

    }

    @Test
    public void testInvalidStateWhenOnSubscribeIsCalled() {
        AbstractUni<Integer> rogue = new AbstractUni<Integer>() {
            @Override
            public void subscribe(UniSubscriber<? super Integer> subscriber) {
                // Do nothing
            }
        };

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        UniSerializedSubscriber<Integer> serialized = new UniSerializedSubscriber<>(rogue, subscriber);

        serialized.onSubscribe(() -> {
        });
        subscriber
                .assertSubscribed()
                .assertFailedWith(IllegalStateException.class, "Invalid transition")
                .assertSignalsReceivedInOrder();

    }

    @Test
    public void testRogueUpstreamSendingMultipleItems() {
        AbstractUni<Integer> rogue = new AbstractUni<Integer>() {
            @Override
            public void subscribe(UniSubscriber<? super Integer> subscriber) {
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
                .assertItem(1)
                .assertSignalsReceivedInOrder();

    }

    @RepeatedTest(100)
    public void testRaceBetweenItemAndFailureWithoutUniserializedSubscriber() {
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

        Awaitility.await().untilAsserted(subscriber::assertTerminated);

        if (subscriber.getFailure() != null) {
            subscriber.assertFailed()
                    .assertFailedWith(IOException.class, "boom");
        } else {
            subscriber
                    .assertCompleted()
                    .assertItem(1);
        }
        subscriber.assertSignalsReceivedInOrder();
    }

    @RepeatedTest(100)
    public void testRaceBetweenItemAndFailureWithUniserializedSubscriber() {
        AtomicReference<UniEmitter<? super Integer>> reference = new AtomicReference<>();
        Uni<Integer> uni = Uni.createFrom().<Integer> emitter(reference::set);

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        uni.subscribe().withSerializedSubscriber(subscriber);

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

        Awaitility.await().untilAsserted(subscriber::assertTerminated);

        if (subscriber.getFailure() != null) {
            subscriber.assertFailed()
                    .assertFailedWith(IOException.class, "boom");
        } else {
            subscriber.assertCompleted()
                    .assertItem(1);
        }
        subscriber.assertSignalsReceivedInOrder();
    }

    @RepeatedTest(100)
    public void testRaceBetweenMultipleItemsWithoutUniserializedSubscriber() {
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

        subscriber.awaitItem();
        assertThat(subscriber.getItem()).isBetween(1, 3);
        subscriber.assertSignalsReceivedInOrder();
    }

    @RepeatedTest(100)
    public void testRaceBetweenMultipleItemsWithUniserializedSubscriber() {
        AtomicReference<UniEmitter<? super Integer>> reference = new AtomicReference<>();
        Uni<Integer> uni = Uni.createFrom().<Integer> emitter(reference::set);

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        uni.subscribe().withSerializedSubscriber(subscriber);

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

        subscriber.awaitItem();
        assertThat(subscriber.getItem()).isBetween(1, 3);
        subscriber.assertSignalsReceivedInOrder();
    }

    @RepeatedTest(100)
    public void testRaceBetweenItemAndCancellationWithoutUniserializedSubscriber() {
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

        if (subscriber.getItem() != null) {
            assertThat(cancelled).isFalse();
            subscriber.assertCompleted().assertItem(1);
        } else {
            subscriber.assertNotTerminated();
        }
        subscriber.assertSignalsReceivedInOrder();
    }

    @RepeatedTest(100)
    public void testRaceBetweenItemAndCancellationWithUniserializedSubscriber() {
        AtomicReference<UniEmitter<? super Integer>> reference = new AtomicReference<>();
        AtomicBoolean cancelled = new AtomicBoolean();
        Uni<Integer> uni = Uni.createFrom().<Integer> emitter(reference::set)
                .onCancellation().invoke(() -> cancelled.set(true));

        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        uni.subscribe().withSerializedSubscriber(subscriber);

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

        if (subscriber.getItem() != null) {
            assertThat(cancelled).isFalse();
            subscriber.assertCompleted().assertItem(1);
        } else {
            subscriber.assertNotTerminated();
        }
        subscriber.assertSignalsReceivedInOrder();
    }

    @Test
    public void testDroppedExceptionsWhenOnFailureThrowsAnException() {
        AtomicReference<Throwable> captured = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(captured::set);

        AtomicReference<UniEmitter<? super Integer>> reference = new AtomicReference<>();
        AbstractUni<Integer> uni = (AbstractUni<Integer>) Uni.createFrom().<Integer> emitter(
                reference::set);

        UniSerializedSubscriber.subscribe(uni, new UniSubscriber<Integer>() {

            @Override
            public Context context() {
                return Context.empty();
            }

            @Override
            public void onSubscribe(UniSubscription subscription) {
                // Do nothing
            }

            @Override
            public void onItem(Integer item) {
                // Do nothing
            }

            @Override
            public void onFailure(Throwable failure) {
                throw new IllegalArgumentException("boom");
            }
        });

        try {
            reference.get().fail(new IOException("I/O"));
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // Expected.
        }
        assertThat(captured.get()).isNotNull().isInstanceOf(CompositeException.class)
                .hasMessageContaining("boom").hasMessageContaining("I/O");
    }

    @Test
    public void testDroppedExceptionsWhenOnItemThrowsAnException() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicReference<Throwable> captured = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(captured::set);

        AtomicReference<UniEmitter<? super Integer>> reference = new AtomicReference<>();
        AbstractUni<Integer> uni = (AbstractUni<Integer>) Uni.createFrom().<Integer> emitter(
                reference::set);

        UniSerializedSubscriber.subscribe(uni, new UniSubscriber<Integer>() {
            @Override
            public Context context() {
                return Context.empty();
            }

            @Override
            public void onSubscribe(UniSubscription subscription) {
                // Do nothing
            }

            @Override
            public void onItem(Integer item) {
                throw new IllegalArgumentException("boom");

            }

            @Override
            public void onFailure(Throwable failure) {
                called.set(true);
            }
        });

        try {
            reference.get().complete(1);
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // Exception expected
        }
        assertThat(captured.get()).isNotNull().isInstanceOf(IllegalArgumentException.class).hasMessageContaining("boom");
        assertThat(called).isFalse();
    }

    @Test
    public void testDroppedExceptionsWhenOnFailureCalledMultipleTimes() {
        AtomicReference<Throwable> received = new AtomicReference<>();
        AtomicReference<Throwable> captured = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(captured::set);

        AtomicReference<UniSubscriber<? super Integer>> sub = new AtomicReference<>();
        AbstractUni<Integer> uni = new AbstractUni<Integer>() {
            @Override
            public void subscribe(UniSubscriber<? super Integer> subscriber) {
                sub.set(subscriber);
            }
        };

        UniSerializedSubscriber.subscribe(uni, new UniSubscriber<Integer>() {
            @Override
            public Context context() {
                return Context.empty();
            }

            @Override
            public void onSubscribe(UniSubscription subscription) {
                // Do nothing
            }

            @Override
            public void onItem(Integer item) {
                // Do nothing
            }

            @Override
            public void onFailure(Throwable failure) {
                received.set(failure);
            }
        });
        sub.get().onSubscribe(mock(UniSubscription.class));

        sub.get().onFailure(new IOException("I/O"));
        assertThat(captured.get()).isNull();
        assertThat(received.get()).isInstanceOf(IOException.class).hasMessageContaining("I/O");

        sub.get().onFailure(new IllegalStateException("boom"));
        assertThat(captured.get()).isNotNull()
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("boom");

        assertThat(received.get()).isInstanceOf(IOException.class).hasMessageContaining("I/O");
    }

    @Test
    public void testDroppedExceptionsWhenOnFailureCalledAfterOnItem() {
        AtomicReference<Throwable> captured = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(captured::set);

        AtomicReference<UniSubscriber<? super Integer>> sub = new AtomicReference<>();
        AbstractUni<Integer> uni = new AbstractUni<Integer>() {
            @Override
            public void subscribe(UniSubscriber<? super Integer> subscriber) {
                sub.set(subscriber);
            }
        };

        UniSerializedSubscriber.subscribe(uni, new UniSubscriber<Integer>() {
            @Override
            public Context context() {
                return Context.empty();
            }

            @Override
            public void onSubscribe(UniSubscription subscription) {
                // Do nothing
            }

            @Override
            public void onItem(Integer item) {
                // Do nothing
            }

            @Override
            public void onFailure(Throwable failure) {
                // Do nothing
            }
        });
        sub.get().onSubscribe(mock(UniSubscription.class));

        sub.get().onItem(1);
        assertThat(captured.get()).isNull();

        sub.get().onFailure(new IllegalStateException("boom"));
        assertThat(captured.get()).isNotNull()
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("boom");
    }

    @Test
    public void testDroppedExceptionsWhenOnFailureCalledAfterCancellation() {
        AtomicReference<Throwable> captured = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(captured::set);

        AtomicReference<UniSubscriber<? super Integer>> sub = new AtomicReference<>();
        AbstractUni<Integer> uni = new AbstractUni<Integer>() {
            @Override
            public void subscribe(UniSubscriber<? super Integer> subscriber) {
                sub.set(subscriber);
            }
        };

        UniSerializedSubscriber.subscribe(uni, new UniSubscriber<Integer>() {
            @Override
            public Context context() {
                return Context.empty();
            }

            @Override
            public void onSubscribe(UniSubscription subscription) {
                subscription.cancel();
            }

            @Override
            public void onItem(Integer item) {
                // Do nothing
            }

            @Override
            public void onFailure(Throwable failure) {
                // Do nothing
            }
        });
        sub.get().onSubscribe(mock(UniSubscription.class));

        sub.get().onFailure(new IllegalStateException("boom"));
        assertThat(captured.get()).isNotNull()
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("boom");
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
