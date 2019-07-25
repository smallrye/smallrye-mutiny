package io.smallrye.reactive.operators;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.subscription.BackPressureFailure;
import io.smallrye.reactive.subscription.BackPressureStrategy;
import io.smallrye.reactive.subscription.MultiEmitter;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static io.smallrye.reactive.subscription.BackPressureStrategy.*;
import static org.assertj.core.api.Assertions.assertThat;

public class MultiCreateFromEmitterTest {


    @Test
    public void testWithDefaultBackPressure() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();
        Multi<Integer> multi = Multi.createFrom().emitter(emitter -> {
            emitter.result(1);
            emitter.result(2);
            emitter.result(3);
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
            emitter.result(1);
            emitter.result(2);
            emitter.result(3);
            emitter.complete();
        });
        multi.subscribe(ts);
        ts.assertSubscribed()
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3);
    }

    @Test
    public void testWithRequests() {
        AtomicInteger terminated = new AtomicInteger();
        Multi.createFrom().emitter(emitter -> {
            emitter.onTermination(terminated::incrementAndGet);
            emitter.result(1);
            emitter.result(2);
            emitter.result(3);
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
            emitter.result(1);
            emitter.result(2);
            emitter.result(3);
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
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer>emitter(emitter -> {
            emitter.onTermination(termination::incrementAndGet);
            emitter.result(1);
            emitter.result(2);
            emitter.result(3);
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
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer>emitter(emitter -> {
            emitter.onTermination(termination::incrementAndGet);
            emitter.result(1);
            emitter.result(2);
            emitter.failure(new IOException("boom"));
            emitter.result(3);
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
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer>emitter(emitter -> {
            emitter.onTermination(termination::incrementAndGet);
            emitter.complete();
        })
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertSubscribed()
                .request(2)
                .assertHasNoResults()
                .request(1)
                .assertHasNoResults()
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
            emitter.result(1);
            emitter.result(2);
            emitter.result(3);
            emitter.complete();

        });

        //noinspection SubscriberImplementation
        source.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
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
        Multi<Integer> multi = Multi.createFrom().emitter(e -> e.result(1).result(2).result(3).complete(), IGNORE);

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertSubscribed()
                .assertReceived(1, 2, 3)
                .request(2)
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWithoutBackPressure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer>emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::result);
            emitter.complete();
        }, BackPressureStrategy.IGNORE).subscribe()
                .withSubscriber(MultiAssertSubscriber.create())
                .assertCompletedSuccessfully();
        assertThat(subscriber.results()).hasSize(1000);

        subscriber = Multi.createFrom().<Integer>emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::result);
            emitter.complete();
        }, BackPressureStrategy.IGNORE).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(5))
                // The request is ignored by the strategy.
                .assertCompletedSuccessfully();
        assertThat(subscriber.results()).hasSize(1000);
    }

    @Test
    public void testLatestBackPressureBehavior() {
        Multi<Integer> multi = Multi.createFrom().emitter(e -> e.result(1).result(2).result(3).complete(), LATEST);

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertSubscribed()
                .assertReceived(1)
                .request(2)
                .assertReceived(1, 3)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWithLatestBackPressure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer>emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::result);
            emitter.complete();
        }, LATEST).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(20))
                .request(Long.MAX_VALUE)
                .assertCompletedSuccessfully();
        // 21 because the 20 first are consumed, and then only the latest is kept.
        assertThat(subscriber.results()).hasSize(21);

        subscriber = Multi.createFrom().<Integer>emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::result);
            emitter.complete();
        }, LATEST).subscribe()
                .withSubscriber(MultiAssertSubscriber.create())
                .request(20)
                .request(Long.MAX_VALUE)
                .assertCompletedSuccessfully();
        assertThat(subscriber.results()).hasSize(1).containsExactly(999);

        Multi.createFrom().<Integer>emitter(MultiEmitter::complete, LATEST)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(20))
                .assertCompletedSuccessfully()
                .assertHasNoResults();

        subscriber = Multi.createFrom().<Integer>emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::result);
            emitter.failure(new IOException("boom"));
        }, LATEST).subscribe()
                .withSubscriber(MultiAssertSubscriber.create())
                .request(20)
                .request(Long.MAX_VALUE)
                .assertHasFailedWith(IOException.class, "boom");
        assertThat(subscriber.results()).hasSize(1).containsExactly(999);

    }

    @Test
    public void testDropBackPressureBehavior() {
        Multi<Integer> multi = Multi.createFrom().emitter(e -> e.result(1).result(2).result(3).complete(), DROP);

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
                .assertHasNoResults()
                .request(2)
                .assertHasNoResults()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWithDropBackPressure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer>emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::result);
            emitter.complete();
        }, BackPressureStrategy.DROP).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(20))
                .request(Long.MAX_VALUE)
                .assertCompletedSuccessfully();
        // 20 because the 20 first are consumed, others are dropped
        assertThat(subscriber.results()).hasSize(20);

        subscriber = Multi.createFrom().<Integer>emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::result);
            emitter.complete();
        }, BackPressureStrategy.DROP).subscribe()
                .withSubscriber(MultiAssertSubscriber.create())
                .request(20)
                .request(Long.MAX_VALUE)
                .assertCompletedSuccessfully();
        assertThat(subscriber.results()).isEmpty();

        Multi.createFrom().<Integer>emitter(MultiEmitter::complete, BackPressureStrategy.DROP)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(20))
                .assertCompletedSuccessfully()
                .assertHasNoResults();

        Multi.createFrom().<Integer>emitter(emitter -> {
            IntStream.range(0, 1000).forEach(emitter::result);
            emitter.failure(new IOException("boom"));
        }, BackPressureStrategy.DROP).subscribe()
                .withSubscriber(MultiAssertSubscriber.create())
                .request(20)
                .request(Long.MAX_VALUE)
                .assertHasFailedWith(IOException.class, "boom")
                .assertHasNoResults();
    }

    @Test
    public void testErrorBackPressureBehavior() {
        Multi<Integer> multi = Multi.createFrom().emitter(e -> e.result(1).result(2).result(3).complete(), ERROR);

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertSubscribed()
                .assertReceived(1)
                .assertHasFailedWith(BackPressureFailure.class, "requests");

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(3))
                .assertSubscribed()
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();

        Multi.createFrom().emitter(MultiEmitter::complete, ERROR)
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertCompletedSuccessfully()
                .assertHasNoResults();
    }
}

