package io.smallrye.mutiny.operators.multi.builders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class EmitterBasedMultiTest {

    @Test
    public void testThatConsumerCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().emitter(null, BackPressureStrategy.BUFFER));
    }

    @Test
    public void testThatStrategyCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().emitter(e -> {
        }, null));
    }

    @Test
    public void testBasicEmitterBehavior() {
        AtomicBoolean terminated = new AtomicBoolean();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(e -> {
            e.onTermination(() -> terminated.set(true));
            e.emit(1).emit(2).emit(3).complete();

            e.fail(new Exception("boom-1"));
        }).subscribe().withSubscriber(AssertSubscriber.create(3));
        subscriber.assertSubscribed()
                .assertItems(1, 2, 3)
                .assertCompleted();
        assertThat(terminated).isTrue();
    }

    @Test
    public void testWithConsumerThrowingException() {
        AtomicBoolean terminated = new AtomicBoolean();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(e -> {
            e.onTermination(() -> terminated.set(true));
            e.emit(1).emit(2).emit(3);

            throw new RuntimeException("boom");
        }).subscribe().withSubscriber(AssertSubscriber.create(3));
        subscriber.assertSubscribed()
                .assertItems(1, 2, 3)
                .assertFailedWith(RuntimeException.class, "boom");
        assertThat(terminated).isTrue();
    }

    @Test
    public void testWithAFailure() {
        AtomicBoolean terminated = new AtomicBoolean();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(e -> {
            e.onTermination(() -> terminated.set(true));
            e.emit(1).emit(2).emit(3).fail(new Exception("boom"));

            e.fail(new Exception("boom-1"));
        }).subscribe().withSubscriber(AssertSubscriber.create(3));
        subscriber.assertSubscribed()
                .assertItems(1, 2, 3)
                .assertFailedWith(Exception.class, "boom");
        assertThat(terminated).isTrue();
    }

    @Test
    public void testTerminationNotCalled() {
        AtomicBoolean terminated = new AtomicBoolean();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(e -> {
            e.onTermination(() -> terminated.set(true));
            e.emit(1).emit(2).emit(3);
        }).subscribe().withSubscriber(AssertSubscriber.create(3));
        subscriber.assertSubscribed()
                .assertItems(1, 2, 3)
                .assertNotTerminated();
        assertThat(terminated).isFalse();
        subscriber.cancel();
        assertThat(terminated).isTrue();
    }

    @Test
    public void testThatEmitterCannotEmitNull() {
        AtomicBoolean terminated = new AtomicBoolean();
        Multi.createFrom().<String> emitter(e -> {
            e.onTermination(() -> terminated.set(true));
            e.emit("a");
            e.emit(null);
        }).subscribe().withSubscriber(AssertSubscriber.create(2))
                .awaitFailure()
                .assertFailedWith(NullPointerException.class, "")
                .assertItems("a");
        assertThat(terminated).isTrue();

        Multi.createFrom().<String> emitter(e -> {
            e.emit("a");
            e.emit(null);
        }, BackPressureStrategy.LATEST)
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .awaitFailure()
                .assertFailedWith(NullPointerException.class, "")
                .assertItems("a");

        Multi.createFrom().<String> emitter(e -> {
            e.emit("a");
            e.emit(null);
        }, BackPressureStrategy.DROP)
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .awaitFailure()
                .assertFailedWith(NullPointerException.class, "")
                .assertItems("a");

        Multi.createFrom().<String> emitter(e -> {
            e.emit("a");
            e.emit(null);
        }, BackPressureStrategy.ERROR)
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .awaitFailure()
                .assertFailedWith(NullPointerException.class, "")
                .assertItems("a");

        Multi.createFrom().<String> emitter(e -> {
            e.emit("a");
            e.emit(null);
        }, BackPressureStrategy.IGNORE)
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .awaitFailure()
                .assertFailedWith(NullPointerException.class, "")
                .assertItems("a");

    }

    @Test
    public void testThatEmitterCannotPropagateNullAsFailure() {
        AtomicBoolean terminated = new AtomicBoolean();
        Multi.createFrom().<String> emitter(e -> {
            e.onTermination(() -> terminated.set(true));
            e.emit("a");
            e.fail(null);
        }).subscribe().withSubscriber(AssertSubscriber.create(2))
                .awaitFailure()
                .assertFailedWith(NullPointerException.class, "")
                .assertItems("a");
        assertThat(terminated).isTrue();

        Multi.createFrom().<String> emitter(e -> {
            e.emit("a");
            e.fail(null);
        }, BackPressureStrategy.LATEST)
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .awaitFailure()
                .assertFailedWith(NullPointerException.class, "")
                .assertItems("a");

        Multi.createFrom().<String> emitter(e -> {
            e.emit("a");
            e.fail(null);
        }, BackPressureStrategy.DROP)
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .awaitFailure()
                .assertFailedWith(NullPointerException.class, "")
                .assertItems("a");

        Multi.createFrom().<String> emitter(e -> {
            e.emit("a");
            e.fail(null);
        }, BackPressureStrategy.ERROR)
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .awaitFailure()
                .assertFailedWith(NullPointerException.class, "")
                .assertItems("a");

        Multi.createFrom().<String> emitter(e -> {
            e.emit("a");
            e.fail(null);
        }, BackPressureStrategy.IGNORE)
                .subscribe().withSubscriber(AssertSubscriber.create(2))
                .awaitFailure()
                .assertFailedWith(NullPointerException.class, "")
                .assertItems("a");

    }

    @Test
    public void testSerializedWithConcurrentEmissions() {
        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(reference::set).subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        await().until(() -> reference.get() != null);

        CountDownLatch latch = new CountDownLatch(2);
        Runnable r1 = () -> {
            latch.countDown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                // Ignore me.
            }
            for (int i = 0; i < 1000; i++) {
                reference.get().emit(i);
            }
        };

        Runnable r2 = () -> {
            latch.countDown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                // Ignore me.
            }
            for (int i = 0; i < 200; i++) {
                reference.get().emit(i);
            }
        };

        ExecutorService service = Executors.newFixedThreadPool(2);
        service.submit(r1);
        service.submit(r2);

        await().until(() -> subscriber.getItems().size() == 1200);
        service.shutdown();
    }

    @Test
    public void testSerializedWithConcurrentEmissionsAndFailure() {
        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(reference::set).subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        await().until(() -> reference.get() != null);

        CountDownLatch latch = new CountDownLatch(2);
        Runnable r1 = () -> {
            latch.countDown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                // Ignore me.
            }
            for (int i = 0; i < 1000; i++) {
                reference.get().emit(i);
            }
        };

        Runnable r2 = () -> {
            latch.countDown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                // Ignore me.
            }
            for (int i = 0; i < 200; i++) {
                reference.get().emit(i);
            }
            reference.get().fail(new Exception("boom"));
        };

        ExecutorService service = Executors.newFixedThreadPool(2);
        service.submit(r1);
        service.submit(r2);

        subscriber
                .awaitFailure()
                .assertFailedWith(Exception.class, "boom");
        service.shutdown();
    }

    @Test
    public void testEmissionAfterCancellation() {
        Multi.createFrom().emitter(e -> e.emit("x"), BackPressureStrategy.IGNORE)
                .subscribe().withSubscriber(new AssertSubscriber<>(1, true))
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        Multi.createFrom().emitter(e -> e.emit("x"), BackPressureStrategy.BUFFER)
                .subscribe().withSubscriber(new AssertSubscriber<>(1, true))
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        Multi.createFrom().emitter(e -> e.emit("x"), BackPressureStrategy.DROP)
                .subscribe().withSubscriber(new AssertSubscriber<>(1, true))
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        Multi.createFrom().emitter(e -> e.emit("x"), BackPressureStrategy.ERROR)
                .subscribe().withSubscriber(new AssertSubscriber<>(1, true))
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        Multi.createFrom().emitter(e -> e.emit("x"), BackPressureStrategy.LATEST)
                .subscribe().withSubscriber(new AssertSubscriber<>(1, true))
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testEmittingNull() {
        Multi.createFrom().emitter(e -> e.emit(null), BackPressureStrategy.IGNORE)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(NullPointerException.class, "emit");

        Multi.createFrom().emitter(e -> e.emit(null), BackPressureStrategy.BUFFER)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(NullPointerException.class, "emit");

        Multi.createFrom().emitter(e -> e.emit(null), BackPressureStrategy.DROP)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(NullPointerException.class, "emit");

        Multi.createFrom().emitter(e -> e.emit(null), BackPressureStrategy.ERROR)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(NullPointerException.class, "emit");

        Multi.createFrom().emitter(e -> e.emit(null), BackPressureStrategy.LATEST)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(NullPointerException.class, "emit");
    }

    @Test
    public void testFailWithNull() {
        Multi.createFrom().emitter(e -> e.fail(null), BackPressureStrategy.IGNORE)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(NullPointerException.class, "fail");

        Multi.createFrom().emitter(e -> e.fail(null), BackPressureStrategy.BUFFER)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(NullPointerException.class, "fail");

        Multi.createFrom().emitter(e -> e.fail(null), BackPressureStrategy.DROP)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(NullPointerException.class, "fail");

        Multi.createFrom().emitter(e -> e.fail(null), BackPressureStrategy.ERROR)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(NullPointerException.class, "fail");

        Multi.createFrom().emitter(e -> e.fail(null), BackPressureStrategy.LATEST)
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(NullPointerException.class, "fail");
    }

    @Test
    public void testLateRegistrationOfOnTerminationAfterCompletion() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(reference::set)
                .subscribe().withSubscriber(AssertSubscriber.create(2));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        reference.get().emit(1);
        reference.get().emit(2);

        subscriber.assertItems(1, 2)
                .assertNotTerminated();

        reference.get().complete();
        subscriber.assertCompleted();

        reference.get().onTermination(() -> called.set(true));
        assertThat(called).isTrue();
    }

    @Test
    public void testLateRegistrationOfOnTerminationAfterFailure() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(reference::set)
                .subscribe().withSubscriber(AssertSubscriber.create(2));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        reference.get().emit(1);
        reference.get().emit(2);

        subscriber.assertItems(1, 2)
                .assertNotTerminated();

        reference.get().fail(new Exception("boom"));
        subscriber.assertFailedWith(Exception.class, "boom");

        reference.get().onTermination(() -> called.set(true));
        assertThat(called).isTrue();
    }

    @Test
    public void testLateRegistrationOfOnTerminationAfterCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(reference::set)
                .subscribe().withSubscriber(AssertSubscriber.create(2));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        reference.get().emit(1);
        reference.get().emit(2);

        subscriber.assertItems(1, 2)
                .assertNotTerminated();

        subscriber.cancel();

        reference.get().onTermination(() -> called.set(true));
        assertThat(called).isTrue();
    }

    @Test
    public void testLateReplacementOfOnTerminationAfterCompletion() {
        AtomicBoolean called1 = new AtomicBoolean();
        AtomicBoolean called2 = new AtomicBoolean();
        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(e -> {
            e.onTermination(() -> called1.set(true));
            reference.set(e);
        })
                .subscribe().withSubscriber(AssertSubscriber.create(2));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        reference.get().emit(1);
        reference.get().emit(2);

        subscriber.assertItems(1, 2)
                .assertNotTerminated();

        assertThat(called1).isFalse();
        reference.get().complete();
        subscriber.assertCompleted();
        assertThat(called1).isTrue();
        assertThat(called2).isFalse();
        reference.get().onTermination(() -> called2.set(true));
        assertThat(called2).isTrue();
    }

    @Test
    public void testReplacementOfOnTerminationBeforeCompletion() {
        AtomicBoolean called1 = new AtomicBoolean();
        AtomicBoolean called2 = new AtomicBoolean();
        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(e -> {
            e.onTermination(() -> called1.set(true));
            reference.set(e);
        })
                .subscribe().withSubscriber(AssertSubscriber.create(2));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        reference.get().emit(1);
        reference.get().emit(2);

        subscriber.assertItems(1, 2)
                .assertNotTerminated();

        assertThat(called1).isFalse();
        reference.get().onTermination(() -> called2.set(true));
        reference.get().complete();
        subscriber.assertCompleted();
        assertThat(called1).isFalse();
        assertThat(called2).isTrue();
    }

    @RepeatedTest(100)
    public void testRegistrationOfOnTerminationAndCompletionRace() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicReference<MultiEmitter<? super Integer>> reference = new AtomicReference<>();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(reference::set)
                .subscribe().withSubscriber(AssertSubscriber.create(2));

        subscriber.assertSubscribed()
                .assertNotTerminated();

        List<Runnable> runnables = new ArrayList<>();
        runnables.add(() -> reference.get().emit(1).complete());
        runnables.add(() -> reference.get().onTermination(() -> called.set(true)));

        Collections.shuffle(runnables);

        runnables.forEach(r -> new Thread(r).start());

        await().untilAsserted(() -> {
            subscriber
                    .assertItems(1)
                    .assertCompleted();
            assertThat(called).isTrue();
        });
    }

    @Test
    public void testThatOnTerminationCallbackCannotBeNull() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(e -> {
            e.onTermination(null);
        })
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.assertFailedWith(IllegalArgumentException.class, "onTermination");

    }
}
