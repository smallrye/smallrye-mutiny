package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.MultiSubscribers.toMultiSubscriber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.processors.PublishProcessor;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.test.Mocks;
import mutiny.zero.flow.adapters.AdaptersToFlow;

public class FlatMapMainSubscriberTest {

    @Test
    public void testThatInvalidRequestAreRejectedByMainSubscriber() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        MultiFlatMapOp.FlatMapMainSubscriber<Integer, Integer> sub = new MultiFlatMapOp.FlatMapMainSubscriber<>(
                toMultiSubscriber(subscriber),
                i -> Multi.createFrom().item(2),
                false,
                4,
                10);

        Multi.createFrom().item(1)
                .subscribe().withSubscriber(sub);

        sub.request(-1);
        subscriber.assertFailedWith(IllegalArgumentException.class, "");
    }

    @Test
    public void testCancellationFromMainSubscriber() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        MultiFlatMapOp.FlatMapMainSubscriber<Integer, Integer> sub = new MultiFlatMapOp.FlatMapMainSubscriber<>(
                toMultiSubscriber(subscriber),
                i -> Multi.createFrom().item(2),
                false,
                4,
                10);

        Multi.createFrom().item(1)
                .subscribe().withSubscriber(sub);

        sub.cancel();
        subscriber.assertNotTerminated();
        sub.cancel();
        sub.onItem(1);
        subscriber.assertHasNotReceivedAnyItem();
    }

    @Test
    public void testThatNoItemsAreDispatchedAfterCompletion() {
        Subscriber<Integer> subscriber = Mocks.subscriber();

        MultiFlatMapOp.FlatMapMainSubscriber<Integer, Integer> sub = new MultiFlatMapOp.FlatMapMainSubscriber<>(
                toMultiSubscriber(subscriber),
                i -> Multi.createFrom().item(2),
                false,
                4,
                10);

        sub.onSubscribe(mock(Subscription.class));
        sub.onNext(1);
        sub.onComplete();

        sub.onNext(2);
        sub.onComplete();

        verify(subscriber).onNext(2);
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void testThatNoItemsAreDispatchedAfterFailure() {
        Subscriber<Integer> subscriber = Mocks.subscriber();

        MultiFlatMapOp.FlatMapMainSubscriber<Integer, Integer> sub = new MultiFlatMapOp.FlatMapMainSubscriber<>(
                toMultiSubscriber(subscriber),
                i -> Multi.createFrom().item(2),
                false,
                4,
                10);

        sub.onSubscribe(mock(Subscription.class));
        sub.onNext(1);
        sub.onError(new Exception("boom"));

        sub.onNext(2);
        sub.onComplete();
        sub.onError(new Exception("boom"));

        verify(subscriber).onNext(2);
        verify(subscriber, never()).onComplete();
        verify(subscriber).onError(any(Throwable.class));
    }

    AbstractMulti<Integer> rogue = new AbstractMulti<Integer>() {
        @Override
        public void subscribe(Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(mock(Subscription.class));
            subscriber.onNext(1);
            subscriber.onNext(2);
        }
    };

    @Test
    public void testInnerOverflow() {
        Multi.createFrom().item(1)
                .onItem().transformToMulti(v -> rogue)
                .merge(1)
                .subscribe().withSubscriber(AssertSubscriber.create(0))
                .assertFailedWith(BackPressureFailure.class, "");
    }

    @Test
    public void testInnerOverflow2() {
        Subscriber<Integer> subscriber = Mocks.subscriber(0);

        MultiFlatMapOp.FlatMapMainSubscriber<Integer, Integer> sub = new MultiFlatMapOp.FlatMapMainSubscriber<>(
                toMultiSubscriber(subscriber),
                i -> rogue,
                false,
                1,
                1);

        sub.onSubscribe(mock(Subscription.class));
        sub.onNext(1);
        sub.done = true;
        sub.drain();

        verify(subscriber).onError(any(BackPressureFailure.class));

    }

    @Test
    public void testInnerOverflowWithWip() {
        Subscriber<Integer> subscriber = Mocks.subscriber(0);

        MultiFlatMapOp.FlatMapMainSubscriber<Integer, Integer> sub = new MultiFlatMapOp.FlatMapMainSubscriber<>(
                toMultiSubscriber(subscriber),
                i -> rogue,
                false,
                1,
                1);

        sub.onSubscribe(mock(Subscription.class));

        sub.wip.getAndIncrement();

        sub.onNext(1);
        assertThat(sub.failures.get()).isInstanceOf(BackPressureFailure.class);

        sub.wip.set(0);
        sub.done = true;
        sub.drain();

        verify(subscriber).onError(any(BackPressureFailure.class));

    }

    @Test
    public void testWhenInnerCompletesAfterOnNextInDrainThenCancels() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().item(1)
                .onItem().transformToMulti(v -> AdaptersToFlow.publisher(pp)).merge()
                .onItem().invoke(v -> {
                    if (v == 1) {
                        pp.onComplete();
                        subscriber.cancel();
                    }
                })
                .subscribe().withSubscriber(subscriber);

        pp.onNext(1);

        subscriber.request(1)
                .assertItems(1);
    }

    @RepeatedTest(50)
    void flatMapInnerErrorShouldNotRaceWithOnNext() {
        AtomicBoolean concurrent = new AtomicBoolean();
        AtomicBoolean violationDetected = new AtomicBoolean();
        AtomicBoolean terminated = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(1);

        Multi.createFrom().items(1, 2)
                .onItem().transformToMultiAndMerge(n -> {
                    if (n == 1) {
                        return Multi.createFrom().<String> emitter(emitter -> {
                            new Thread(() -> {
                                try {
                                    latch.await();
                                } catch (InterruptedException ignored) {
                                }
                                for (int i = 0; i < 100; i++) {
                                    emitter.emit("item-" + i);
                                }
                                emitter.complete();
                            }).start();
                        });
                    } else {
                        return Multi.createFrom().<String> emitter(emitter -> {
                            new Thread(() -> {
                                try {
                                    latch.await();
                                } catch (InterruptedException ignored) {
                                }
                                emitter.fail(new RuntimeException("inner error"));
                            }).start();
                        });
                    }
                })
                .subscribe().withSubscriber(new MultiSubscriber<String>() {
                    @Override
                    public void onSubscribe(Flow.Subscription s) {
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onItem(String item) {
                        if (!concurrent.compareAndSet(false, true)) {
                            violationDetected.set(true);
                        }
                        Thread.yield();
                        concurrent.set(false);
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                        if (!concurrent.compareAndSet(false, true)) {
                            violationDetected.set(true);
                        }
                        concurrent.set(false);
                        terminated.set(true);
                    }

                    @Override
                    public void onCompletion() {
                        terminated.set(true);
                    }
                });

        latch.countDown();
        await().atMost(Duration.ofSeconds(5)).untilTrue(terminated);
        assertThat(violationDetected).isFalse();
    }
}
