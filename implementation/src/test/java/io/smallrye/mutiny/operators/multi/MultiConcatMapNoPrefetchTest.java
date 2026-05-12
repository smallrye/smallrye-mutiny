package io.smallrye.mutiny.operators.multi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.MultiFlatten;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

class MultiConcatMapNoPrefetchTest {

    AtomicInteger upstreamRequestCount;
    Multi<Integer> upstream;

    @BeforeEach
    void setUp() {
        upstreamRequestCount = new AtomicInteger();
        upstream = Multi.createFrom().generator(() -> upstreamRequestCount, (counter, emitter) -> {
            int requestCount = counter.getAndIncrement();
            emitter.emit(requestCount);
            return counter;
        });
    }

    @ParameterizedTest
    @MethodSource("argsTransformToUni")
    void testTransformToUni(boolean prefetch, int[] upstreamRequests) {
        Multi<Integer> result = upstream.onItem()
                .transformToUni(integer -> Uni.createFrom().item(integer)
                        .onItem().delayIt().by(Duration.ofMillis(10)))
                .concatenate(prefetch);
        AssertSubscriber<Integer> ts = new AssertSubscriber<>(5);
        result.runSubscriptionOn(Infrastructure.getDefaultExecutor()).subscribe(ts);
        ts.request(5);
        ts.awaitItems(10);
        assertThat(upstreamRequestCount).hasValue(upstreamRequests[0]);
        ts.request(1);
        ts.awaitItems(11);
        assertThat(upstreamRequestCount).hasValue(upstreamRequests[1]);
        ts.request(1);
        ts.awaitItems(12);
        assertThat(upstreamRequestCount).hasValue(upstreamRequests[2]);
    }

    private static Stream<Arguments> argsTransformToUni() {
        return Stream.of(
                Arguments.of(true, new int[] { 11, 12, 13 }),
                Arguments.of(false, new int[] { 10, 11, 12 }));
    }

    @ParameterizedTest
    @MethodSource("argsTransformToMulti")
    void testTransformToMulti(boolean prefetch, int[] upstreamRequests) {
        Multi<Integer> result = upstream.onItem()
                .transformToMulti(i -> Multi.createFrom().items(i, i))
                .concatenate(prefetch);
        AssertSubscriber<Integer> ts = new AssertSubscriber<>();
        result.runSubscriptionOn(Infrastructure.getDefaultExecutor()).subscribe(ts);
        ts.request(10);
        ts.awaitItems(10);
        assertThat(upstreamRequestCount).hasValue(upstreamRequests[0]);
        ts.request(1);
        ts.awaitItems(11);
        assertThat(upstreamRequestCount).hasValue(upstreamRequests[1]);
        ts.request(1);
        ts.awaitItems(12);
        assertThat(upstreamRequestCount).hasValue(upstreamRequests[2]);
    }

    private static Stream<Arguments> argsTransformToMulti() {
        return Stream.of(
                Arguments.of(true, new int[] { 6, 6, 7 }),
                Arguments.of(false, new int[] { 5, 6, 6 }));
    }

    @ParameterizedTest
    @MethodSource("argsNoPrefetchPostponeFailure")
    void testNoPrefetchPostponeFailure(boolean postponeFailure, Integer[] expectedItems, int expectedUpstreamRequest) {
        AtomicInteger itemCounter = new AtomicInteger();
        MultiFlatten<Integer, Integer> flatten = upstream.onItem().transformToMulti(i -> {
            if (itemCounter.incrementAndGet() == 3) {
                return Multi.createFrom().emitter(e -> {
                    e.emit(i);
                    e.fail(new IllegalArgumentException("3rd item"));
                });
            }
            return Multi.createFrom().items(i, i);
        });
        Multi<Integer> result = (postponeFailure ? flatten.collectFailures() : flatten).concatenate(false);
        AssertSubscriber<Integer> ts = new AssertSubscriber<>(5);
        result.runSubscriptionOn(Infrastructure.getDefaultExecutor()).subscribe(ts);
        ts.request(5);
        ts.awaitItems(expectedItems.length);
        ts.assertItems(expectedItems);
        assertThat(upstreamRequestCount).hasValue(expectedUpstreamRequest);
    }

    private static Stream<Arguments> argsNoPrefetchPostponeFailure() {
        return Stream.of(
                Arguments.of(true, new Integer[] { 0, 0, 1, 1, 2, 3, 3, 4, 4, 5 }, 6),
                Arguments.of(false, new Integer[] { 0, 0, 1, 1, 2 }, 3));
    }

    @Test
    void testNoPrefetchWithConcatMapEmptyMulti() {
        Multi<Integer> result = upstream.select().first(20).onItem()
                .transformToMulti(i -> Multi.createFrom().<Integer> empty())
                .concatenate();
        AssertSubscriber<Integer> ts = new AssertSubscriber<>(5);
        result.runSubscriptionOn(Infrastructure.getDefaultExecutor()).subscribe(ts);
        ts.request(5);
        ts.awaitCompletion();
        assertThat(upstreamRequestCount).hasValueGreaterThan(10);
        ts.assertHasNotReceivedAnyItem().assertCompleted();
    }

    @Test
    void testNoPrefetchWithConcatMapContainingEmpty() {
        Multi<Integer> result = upstream.onItem()
                .transformToMulti(i -> (i % 3 == 0) ? Multi.createFrom().empty() : Multi.createFrom().item(i))
                .concatenate();
        AssertSubscriber<Integer> ts = new AssertSubscriber<>(5);
        result.runSubscriptionOn(Infrastructure.getDefaultExecutor()).subscribe(ts);
        ts.awaitSubscription();
        ts.request(5);
        ts.awaitItems(10);
        assertThat(upstreamRequestCount).hasValueGreaterThan(10);
        ts.assertItems(1, 2, 4, 5, 7, 8, 10, 11, 13, 14);
        ts.request(1);
        ts.awaitItems(11);
        assertThat(upstreamRequestCount).hasValueGreaterThan(11);
        ts.assertLastItem(16);
    }

    @Test
    void testNoPrefetchWithConcatMapEmptyUni() {
        Multi<Integer> result = upstream.select().first(20).onItem()
                .transformToUni(i -> Uni.createFrom().<Integer> nullItem())
                .concatenate();
        AssertSubscriber<Integer> ts = new AssertSubscriber<>(5);
        result.runSubscriptionOn(Infrastructure.getDefaultExecutor()).subscribe(ts);
        ts.request(5);
        ts.awaitCompletion();
        assertThat(upstreamRequestCount).hasValueGreaterThan(10);
        ts.assertHasNotReceivedAnyItem().assertCompleted();
    }

    @Test
    void testMapperReturningNull() {
        Multi<Integer> result = upstream
                .onItem().transformToMulti(i -> {
                    if (i == 0) {
                        return Multi.createFrom().items(i);
                    }
                    return null;
                })
                .concatenate();
        AssertSubscriber<Integer> ts = new AssertSubscriber<>(2);
        result.subscribe(ts);
        ts.assertItems(0);
        ts.assertFailedWith(NullPointerException.class);
    }

    @Test
    void testMapperReturningNullpostponeFailure() {
        Multi<Integer> result = upstream.select().first(5)
                .onItem().transformToUni(i -> (Uni<Integer>) null)
                .collectFailures()
                .concatenate();
        AssertSubscriber<Integer> ts = new AssertSubscriber<>(5);
        result.subscribe(ts);
        ts.assertHasNotReceivedAnyItem().assertFailedWith(CompositeException.class);
    }

    @Test
    void testUpstreamFailure() {
        Multi<Integer> result = upstream
                .onItem().failWith(() -> new RuntimeException("boom"))
                .onItem().transformToUni(i -> Uni.createFrom().item(i))
                .concatenate();
        AssertSubscriber<Integer> ts = new AssertSubscriber<>(1);
        result.subscribe(ts);
        ts.assertHasNotReceivedAnyItem().assertFailedWith(RuntimeException.class);
    }

    @Test
    void testUpstreamFailurepostponeFailure() {
        Multi<Integer> result = upstream.select().first(5)
                .onItem().failWith(() -> new RuntimeException("boom"))
                .onItem().transformToUni(i -> Uni.createFrom().item(i))
                .collectFailures()
                .concatenate();
        AssertSubscriber<Integer> ts = new AssertSubscriber<>(1);
        result.subscribe(ts);
        ts.assertHasNotReceivedAnyItem().assertFailedWith(RuntimeException.class);
    }

    @Test
    void testCancelledSubscription() {
        CompletableFuture<Integer> inner = new CompletableFuture<>();
        AtomicBoolean upstreamCancelled = new AtomicBoolean();
        AtomicBoolean innerCancelled = new AtomicBoolean();
        Multi<Integer> result = upstream
                .onCancellation().invoke(() -> upstreamCancelled.set(true))
                .onItem().transformToUni(i -> Uni.createFrom().completionStage(inner)
                        .onCancellation().invoke(() -> innerCancelled.set(true)))
                .concatenate();
        AssertSubscriber<Integer> ts = new AssertSubscriber<>(1);
        result.subscribe(ts);
        ts.cancel();
        assertThat(upstreamCancelled).isTrue();
        assertThat(innerCancelled).isTrue();
        ts.assertHasNotReceivedAnyItem();
    }

    @Test
    void testCancelledSubscriptionAfterTermination() {
        upstream = Multi.createFrom().generator(() -> upstreamRequestCount, (counter, emitter) -> {
            int requestCount = counter.getAndIncrement();
            emitter.emit(requestCount);
            emitter.complete();
            return counter;
        });
        CompletableFuture<Integer> inner = new CompletableFuture<>();
        AtomicBoolean innerCancelled = new AtomicBoolean();
        AtomicBoolean downstreamCancelled = new AtomicBoolean();
        Multi<Integer> result = upstream
                .onItem().transformToUni(i -> Uni.createFrom().completionStage(inner)
                        .emitOn(Infrastructure.getDefaultExecutor())
                        .onCancellation().invoke(() -> innerCancelled.set(true)))
                .concatenate()
                .onCancellation().invoke(() -> downstreamCancelled.set(true));
        AssertSubscriber<Integer> ts = new AssertSubscriber<>(1);
        result.subscribe(ts);
        ts.cancel();
        assertThat(innerCancelled).isTrue();
        assertThat(downstreamCancelled).isTrue();
    }

    @Test
    void testInnerCompleteSubscriptionAfterTermination() {
        upstream = Multi.createFrom().generator(() -> upstreamRequestCount, (counter, emitter) -> {
            int requestCount = counter.getAndIncrement();
            emitter.emit(requestCount);
            emitter.complete();
            return counter;
        });
        CompletableFuture<Integer> inner = new CompletableFuture<>();
        Multi<Integer> result = upstream
                .onItem().transformToUni(i -> Uni.createFrom().completionStage(inner)
                        .emitOn(Infrastructure.getDefaultExecutor()))
                .concatenate();
        AssertSubscriber<Integer> ts = new AssertSubscriber<>(1);
        result.subscribe(ts);
        inner.complete(0);
        ts.awaitCompletion();
    }

    @Test
    void testPostponedFailureWithBoundaryDemand() {
        AssertSubscriber<String> sub = Multi.createFrom().items(1, 2, 3)
                .onItem().transformToMulti(n -> Multi.createFrom().<String> emitter(emitter -> {
                    emitter.emit("foo");
                    emitter.emit("bar");
                    emitter.fail(new Exception("baz"));
                })).collectFailures().concatenate()
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(2);
        sub.assertNotTerminated().assertItems("foo", "bar");

        sub.request(2);
        sub.assertNotTerminated().assertItems("foo", "bar", "foo", "bar");

        sub.request(3);
        sub.assertTerminated().assertItems("foo", "bar", "foo", "bar", "foo", "bar");

        sub.assertFailedWith(CompositeException.class);
        assertThat(((CompositeException) sub.getFailure()).getCauses())
                .hasSize(3)
                .allMatch(err -> "baz".equals(err.getMessage()));
    }

    @Test
    void testUpfrontCompletion() {
        AssertSubscriber<Integer> sub = Multi.createFrom().empty()
                .onItem().transformToMultiAndConcatenate(n -> Multi.createFrom().items(1, 2, 3))
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.assertCompleted().assertHasNotReceivedAnyItem();
    }

    @Test
    void rejectBadRequests() {
        Multi<Integer> multi = upstream.onItem().transformToMultiAndConcatenate(n -> Multi.createFrom().item(1));

        AssertSubscriber<Integer> sub = multi.subscribe().withSubscriber(AssertSubscriber.create());
        sub.request(0L);
        sub.assertHasNotReceivedAnyItem().assertFailedWith(IllegalArgumentException.class,
                "Invalid request number, must be greater than 0");

        sub = multi.subscribe().withSubscriber(AssertSubscriber.create());
        sub.request(-10L);
        sub.assertHasNotReceivedAnyItem().assertFailedWith(IllegalArgumentException.class,
                "Invalid request number, must be greater than 0");
    }

    @Test
    void testCancellation() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AssertSubscriber<Integer> sub = Multi.createFrom().ticks().every(Duration.ofMillis(100))
                .onCancellation().invoke(() -> cancelled.set(true))
                .onItem().transformToMultiAndConcatenate(tick -> Multi.createFrom().items(1, 2, 3))
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        await().atMost(Duration.ofSeconds(3)).until(() -> sub.getItems().size() > 12);
        sub.cancel();
        await().atMost(Duration.ofSeconds(3)).until(cancelled::get);
        assertThat(sub.isCancelled()).isTrue();
        assertThat(sub.hasCompleted()).isFalse();
        assertThat(sub.getItems()).contains(1, 2, 3);
    }

    @Test
    void simpleConcatMap() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(1, 3)
                .onItem().transformToMultiAndConcatenate(n -> Multi.createFrom().items(n * 10, n * 20))
                .subscribe().withSubscriber(AssertSubscriber.create());
        sub.request(1);
        sub.assertItems(10);
        sub.request(2);
        sub.assertItems(10, 20, 20);
        sub.request(Long.MAX_VALUE);
        sub.assertItems(10, 20, 20, 40);
        sub.assertCompleted();
    }

    @RepeatedTest(1000)
    void earlyRequestWithNullInner() {
        var sub = Multi.createFrom().items(1, 2, 3)
                .emitOn(Infrastructure.getDefaultExecutor())
                .onItem().call(i -> Uni.createFrom().item(i))
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(1);
        sub.awaitNextItems(1, Duration.ofSeconds(1));
        sub.cancel();
    }

    @RepeatedTest(50)
    void innerOnSubscribeDemandDoubleRequestRace() throws Exception {
        AtomicLong totalInnerRequested = new AtomicLong();
        CyclicBarrier barrier = new CyclicBarrier(2);

        Multi<Integer> result = Multi.createFrom().item(1)
                .emitOn(Infrastructure.getDefaultExecutor())
                .onItem().<Integer> transformToMulti(item -> subscriber -> {
                    subscriber.onSubscribe(new Flow.Subscription() {
                        @Override
                        public void request(long n) {
                            totalInnerRequested.addAndGet(n);
                        }

                        @Override
                        public void cancel() {
                        }
                    });
                })
                .concatenate();

        AssertSubscriber<Integer> sub = AssertSubscriber.create();
        result.subscribe().withSubscriber(sub);

        Thread t1 = new Thread(() -> {
            try {
                barrier.await();
            } catch (Exception ignored) {
            }
            sub.request(5);
        });
        Thread t2 = new Thread(() -> {
            try {
                barrier.await();
            } catch (Exception ignored) {
            }
            sub.request(5);
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(totalInnerRequested.get()).isGreaterThan(0));
        assertThat(totalInnerRequested.get()).isLessThanOrEqualTo(10);
    }

    @Test
    void addFailureSequentialInnerThenOuterFailure() {
        AtomicReference<Flow.Subscriber<? super Integer>> outerSubRef = new AtomicReference<>();
        AtomicReference<Flow.Subscriber<? super Integer>> innerSubRef = new AtomicReference<>();
        AtomicReference<Throwable> downstreamFailure = new AtomicReference<>();

        Multi<Integer> source = Multi.createFrom().<Integer> publisher(outerSubRef::set);

        source.onItem().<Integer> transformToMulti(n -> subscriber -> innerSubRef.set(subscriber))
                .collectFailures()
                .concatenate()
                .subscribe().withSubscriber(new MultiSubscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription s) {
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onItem(Integer item) {
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                        downstreamFailure.set(failure);
                    }

                    @Override
                    public void onCompletion() {
                    }
                });

        Flow.Subscriber<? super Integer> outerSubscriber = outerSubRef.get();
        outerSubscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        });
        outerSubscriber.onNext(1);

        Flow.Subscriber<? super Integer> innerSubscriber = innerSubRef.get();
        assertThat(innerSubscriber).isNotNull();
        innerSubscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        });

        RuntimeException innerError = new RuntimeException("inner");
        RuntimeException outerError = new RuntimeException("outer");

        innerSubscriber.onError(innerError);
        outerSubscriber.onError(outerError);

        assertThat(downstreamFailure.get()).isNotNull();
        assertThat(downstreamFailure.get()).isInstanceOf(CompositeException.class);
        CompositeException composite = (CompositeException) downstreamFailure.get();
        assertThat(composite.getCauses()).hasSize(2);
        assertThat(composite.getCauses()).contains(innerError, outerError);
    }

    @RepeatedTest(100)
    void addFailureRaceBetweenInnerAndOuterOnFailure() throws Exception {
        AtomicReference<Flow.Subscriber<? super Integer>> outerSubRef = new AtomicReference<>();
        AtomicReference<Flow.Subscriber<? super Integer>> innerSubRef = new AtomicReference<>();
        AtomicReference<Throwable> downstreamFailure = new AtomicReference<>();
        AtomicReference<Throwable> droppedFailure = new AtomicReference<>();
        CyclicBarrier barrier = new CyclicBarrier(2);

        Infrastructure.setDroppedExceptionHandler(droppedFailure::set);
        try {
            Multi<Integer> source = Multi.createFrom().<Integer> publisher(outerSubRef::set);

            source.onItem().<Integer> transformToMulti(n -> subscriber -> innerSubRef.set(subscriber))
                    .collectFailures()
                    .concatenate()
                    .subscribe().withSubscriber(new MultiSubscriber<>() {
                        @Override
                        public void onSubscribe(Flow.Subscription s) {
                            s.request(Long.MAX_VALUE);
                        }

                        @Override
                        public void onItem(Integer item) {
                        }

                        @Override
                        public void onFailure(Throwable failure) {
                            downstreamFailure.set(failure);
                        }

                        @Override
                        public void onCompletion() {
                        }
                    });

            Flow.Subscriber<? super Integer> outerSubscriber = outerSubRef.get();
            outerSubscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
            outerSubscriber.onNext(1);

            await().atMost(Duration.ofSeconds(2)).until(() -> innerSubRef.get() != null);
            Flow.Subscriber<? super Integer> innerSubscriber = innerSubRef.get();
            innerSubscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });

            RuntimeException innerError = new RuntimeException("inner");
            RuntimeException outerError = new RuntimeException("outer");

            Thread t1 = new Thread(() -> {
                try {
                    barrier.await();
                } catch (Exception ignored) {
                }
                innerSubscriber.onError(innerError);
            });
            Thread t2 = new Thread(() -> {
                try {
                    barrier.await();
                } catch (Exception ignored) {
                }
                outerSubscriber.onError(outerError);
            });
            t1.start();
            t2.start();
            t1.join();
            t2.join();

            await().atMost(Duration.ofSeconds(2)).until(() -> downstreamFailure.get() != null);

            Throwable failure = downstreamFailure.get();
            if (failure instanceof CompositeException) {
                assertThat(((CompositeException) failure).getCauses()).hasSize(2);
                assertThat(((CompositeException) failure).getCauses()).contains(innerError, outerError);
            } else {
                assertThat(failure).isIn(innerError, outerError);
                assertThat(droppedFailure.get()).isNotNull();
            }
        } finally {
            Infrastructure.resetDroppedExceptionHandler();
        }
    }

    @Test
    void cancelShouldPreventFurtherItemDelivery() {
        AtomicInteger itemCount = new AtomicInteger();
        AtomicReference<Flow.Subscription> subRef = new AtomicReference<>();
        AtomicReference<Flow.Subscriber<? super Integer>> upstreamSubscriberRef = new AtomicReference<>();

        Multi<Integer> source = Multi.createFrom().<Integer> publisher(upstreamSubscriberRef::set);

        source.onItem().transformToMulti(n -> Multi.createFrom().items(n))
                .concatenate()
                .subscribe().withSubscriber(new MultiSubscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription s) {
                        subRef.set(s);
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onItem(Integer item) {
                        itemCount.incrementAndGet();
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                    }

                    @Override
                    public void onCompletion() {
                    }
                });

        Flow.Subscriber<? super Integer> upstreamSubscriber = upstreamSubscriberRef.get();
        upstreamSubscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        });

        upstreamSubscriber.onNext(1);
        upstreamSubscriber.onNext(2);
        assertThat(itemCount.get()).isEqualTo(2);

        subRef.get().cancel();

        upstreamSubscriber.onNext(3);
        upstreamSubscriber.onNext(4);
        upstreamSubscriber.onNext(5);

        assertThat(itemCount.get()).isEqualTo(2);
    }
}
