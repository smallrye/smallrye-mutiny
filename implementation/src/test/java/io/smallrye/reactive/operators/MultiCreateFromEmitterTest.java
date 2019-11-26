package io.smallrye.reactive.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.subscription.BackPressureFailure;
import io.smallrye.reactive.subscription.BackPressureStrategy;
import io.smallrye.reactive.subscription.MultiEmitter;
import io.smallrye.reactive.test.MultiAssertSubscriber;

public class MultiCreateFromEmitterTest {

    @Test
    public void testWithDefaultBackPressure() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();
        Multi<Integer> multi = Multi.createFrom().emitter(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
            emitter.complete();
        });
        multi.subscribe(ts);
        ts.assertSubscribed()
                .request(Long.MAX_VALUE)
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3);
    }

    @Test
    public void testRequestsAtSubscription() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(Long.MAX_VALUE);
        Multi<Integer> multi = Multi.createFrom().emitter(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
            emitter.complete();
        });
        multi.subscribe(ts);
        ts.assertSubscribed()
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3);
    }

    @Test
    public void testWhenConsumerThrowsAnException() {
        AtomicBoolean onTerminationCalled = new AtomicBoolean();
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();
        Multi<Integer> multi = Multi.createFrom().emitter(emitter -> {
            emitter.onTermination(() -> onTerminationCalled.set(true));
            emitter.emit(1);
            emitter.emit(2);
            throw new IllegalStateException("boom");
        });
        multi.subscribe(ts);
        ts.assertSubscribed()
                .request(Long.MAX_VALUE)
                .assertHasFailedWith(IllegalStateException.class, "boom")
                .assertReceived(1, 2);
        assertThat(onTerminationCalled).isTrue();
    }

    @Test
    public void testWithRequests() {
        AtomicInteger terminated = new AtomicInteger();
        Multi.createFrom().emitter(emitter -> {
            emitter.onTermination(terminated::incrementAndGet);
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
            emitter.complete();
        })
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertSubscribed()
                .request(2)
                .assertNotTerminated()
                .assertReceived(1, 2)
                .request(2)
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();

        assertThat(terminated).hasValue(1);
    }

    @Test
    public void testCancellation() {
        AtomicInteger cancelled = new AtomicInteger();
        MultiAssertSubscriber<Object> subscriber = Multi.createFrom().emitter(emitter -> {
            emitter.onTermination(cancelled::incrementAndGet);
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
            emitter.complete();
        })
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertSubscribed()
                .request(2)
                .assertNotTerminated()
                .assertReceived(1, 2)
                .cancel()
                .request(1)
                .assertReceived(1, 2)
                .assertNotTerminated();

        assertThat(cancelled).hasValue(1);
        subscriber.cancel();
        assertThat(cancelled).hasValue(1);
    }

    @Test
    public void testOnTerminationOnCompletion() {
        AtomicInteger termination = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(emitter -> {
            emitter.onTermination(termination::incrementAndGet);
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
            emitter.complete();
        })
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertSubscribed()
                .request(2)
                .assertNotTerminated()
                .assertReceived(1, 2)
                .request(1)
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();

        assertThat(termination).hasValue(1);
        subscriber.cancel();
        assertThat(termination).hasValue(1);
    }

    @Test
    public void testOnTerminationOnFailure() {
        AtomicInteger termination = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(emitter -> {
            emitter.onTermination(termination::incrementAndGet);
            emitter.emit(1);
            emitter.emit(2);
            emitter.fail(new IOException("boom"));
            emitter.emit(3);
            emitter.complete();
        })
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertSubscribed()
                .request(2)
                .assertReceived(1, 2)
                .assertHasFailedWith(IOException.class, "boom")
                .request(1)
                .assertReceived(1, 2);

        assertThat(termination).hasValue(1);
        subscriber.cancel();
        assertThat(termination).hasValue(1);
    }

    @Test
    public void testOnTerminationWhenEmpty() {
        AtomicInteger termination = new AtomicInteger();
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(emitter -> {
            emitter.onTermination(termination::incrementAndGet);
            emitter.complete();
        })
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertSubscribed()
                .request(2)
                .assertHasNotReceivedAnyItem()
                .request(1)
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();

        assertThat(termination).hasValue(1);
        subscriber.cancel();
        assertThat(termination).hasValue(1);
    }

    @Test
    public void testCancellationWhenSubscriberThrowAnException() {
        AtomicBoolean errored = new AtomicBoolean();
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicInteger numberOfResults = new AtomicInteger();

        Multi<Integer> source = Multi.createFrom().emitter(emitter -> {
            emitter.onTermination(() -> cancelled.set(true));
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
            emitter.complete();

        });

        //noinspection SubscriberImplementation
        source.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                System.out.println(s);
                s.request(10);
            }

            @Override
            public void onNext(Integer integer) {
                numberOfResults.incrementAndGet();
                throw new RuntimeException("BOOM!");
            }

            @Override
            public void onError(Throwable t) {
                errored.set(true);
            }

            @Override
            public void onComplete() {
                // ignored.
            }
        });

        assertThat(errored).isFalse();
        assertThat(cancelled).isTrue();
        assertThat(numberOfResults.get()).isEqualTo(1);
    }

    @Test
    public void testIgnoreBackPressureBehavior() {
        Multi<Integer> multi = Multi.createFrom().emitter(e -> e.emit(1).emit(2).emit(3).complete(),
                BackPressureStrategy.IGNORE);

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertSubscribed()
                .assertReceived(1, 2, 3)
                .request(2)
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWithoutBackPressure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::emit);
            emitter.complete();
        }, BackPressureStrategy.IGNORE).subscribe()
                .withSubscriber(MultiAssertSubscriber.create())
                .assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(1000);

        subscriber = Multi.createFrom().<Integer> emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::emit);
            emitter.complete();
        }, BackPressureStrategy.IGNORE).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(5))
                // The request is ignored by the strategy.
                .assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(1000);
    }

    @Test
    public void testLatestBackPressureBehavior() {
        Multi<Integer> multi = Multi.createFrom().emitter(e -> e.emit(1).emit(2).emit(3).complete(),
                BackPressureStrategy.LATEST);

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertSubscribed()
                .assertReceived(1)
                .request(2)
                .assertReceived(1, 3)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWithLatestBackPressure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::emit);
            emitter.complete();
        }, BackPressureStrategy.LATEST).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(20))
                .request(Long.MAX_VALUE)
                .assertCompletedSuccessfully();
        // 21 because the 20 first are consumed, and then only the latest is kept.
        assertThat(subscriber.items()).hasSize(21);

        subscriber = Multi.createFrom().<Integer> emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::emit);
            emitter.complete();
        }, BackPressureStrategy.LATEST).subscribe()
                .withSubscriber(MultiAssertSubscriber.create())
                .request(20)
                .request(Long.MAX_VALUE)
                .assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(1).containsExactly(999);

        Multi.createFrom().<Integer> emitter(MultiEmitter::complete, BackPressureStrategy.LATEST)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(20))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();

        subscriber = Multi.createFrom().<Integer> emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::emit);
            emitter.fail(new IOException("boom"));
        }, BackPressureStrategy.LATEST).subscribe()
                .withSubscriber(MultiAssertSubscriber.create())
                .request(20)
                .request(Long.MAX_VALUE)
                .assertHasFailedWith(IOException.class, "boom");
        assertThat(subscriber.items()).hasSize(1).containsExactly(999);

    }

    @Test
    public void testDropBackPressureBehavior() {
        Multi<Integer> multi = Multi.createFrom().emitter(e -> e.emit(1).emit(2).emit(3).complete(), BackPressureStrategy.DROP);

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertSubscribed()
                .assertReceived(1)
                .request(2)
                .assertReceived(1)
                .assertCompletedSuccessfully();

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(3))
                .assertSubscribed()
                .assertReceived(1, 2, 3)
                .request(2)
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertSubscribed()
                .assertHasNotReceivedAnyItem()
                .request(2)
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWithDropBackPressure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::emit);
            emitter.complete();
        }, BackPressureStrategy.DROP).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(20))
                .request(Long.MAX_VALUE)
                .assertCompletedSuccessfully();
        // 20 because the 20 first are consumed, others are dropped
        assertThat(subscriber.items()).hasSize(20);

        subscriber = Multi.createFrom().<Integer> emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::emit);
            emitter.complete();
        }, BackPressureStrategy.DROP).subscribe()
                .withSubscriber(MultiAssertSubscriber.create())
                .request(20)
                .request(Long.MAX_VALUE)
                .assertCompletedSuccessfully();
        assertThat(subscriber.items()).isEmpty();

        Multi.createFrom().<Integer> emitter(MultiEmitter::complete, BackPressureStrategy.DROP)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(20))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();

        Multi.createFrom().<Integer> emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::emit);
            emitter.fail(new IOException("boom"));
        }, BackPressureStrategy.DROP).subscribe()
                .withSubscriber(MultiAssertSubscriber.create())
                .request(20)
                .request(Long.MAX_VALUE)
                .assertHasFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testErrorBackPressureBehavior() {
        Multi<Integer> multi = Multi.createFrom().emitter(e -> e.emit(1).emit(2).emit(3).complete(),
                BackPressureStrategy.ERROR);

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertSubscribed()
                .assertReceived(1)
                .assertHasFailedWith(BackPressureFailure.class, "requests");

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(3))
                .assertSubscribed()
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();

        Multi.createFrom().emitter(MultiEmitter::complete, BackPressureStrategy.ERROR)
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }
}
