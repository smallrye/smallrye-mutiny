package mutiny.zero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

class ZeroPublisherTest {

    @Nested
    @DisplayName("Publisher from iterables")
    class Iterables {

        @Test
        @DisplayName("Items from a null collection")
        void fromNull() {
            Object[] array = null;
            Assertions.assertThrows(NullPointerException.class, () -> ZeroPublisher.fromItems(array));

            List<?> collection = null;
            Assertions.assertThrows(NullPointerException.class, () -> ZeroPublisher.fromIterable(collection));
        }

        @Test
        @DisplayName("Empty collection")
        void fromEmpty() {
            AssertSubscriber<Object> sub = AssertSubscriber.create();
            ZeroPublisher.fromIterable(Collections.emptyList()).subscribe(sub);

            sub.assertHasNotReceivedAnyItem().assertCompleted();
        }

        @Test
        @DisplayName("Items from a collection (request batches)")
        void fromItemsInBatches() {
            AssertSubscriber<Object> sub = AssertSubscriber.create();
            ZeroPublisher.fromItems(1, 2, 3).subscribe(sub);

            sub.assertNotTerminated();
            sub.request(1);
            sub.assertItems(1);
            sub.assertNotTerminated();
            sub.request(10);
            sub.assertItems(1, 2, 3);
            sub.assertCompleted();
        }

        @Test
        @DisplayName("Items from a collection (unbounded initial request)")
        void fromItemsUnbounded() {
            AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
            ZeroPublisher.fromItems(1, 2, 3).subscribe(sub);

            sub.assertItems(1, 2, 3);
            sub.assertCompleted();
        }

        @Test
        @DisplayName("Items from an array (unbounded initial request)")
        void fromArrayUnbounded() {
            AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
            Integer[] array = { 1, 2, 3 };
            ZeroPublisher.fromItems(array).subscribe(sub);

            sub.assertItems(1, 2, 3);
            sub.assertCompleted();
        }

        @Test
        @DisplayName("Items from a collection (midway cancellation)")
        void fromItemsCancellation() {
            AssertSubscriber<Object> sub = AssertSubscriber.create();
            ZeroPublisher.fromItems(1, 2, 3).subscribe(sub);

            sub.assertNotTerminated();
            sub.request(1);
            sub.assertItems(1);

            sub.cancel();
            sub.assertNotTerminated();
        }

        @Test
        @DisplayName("Items from a collection (unbounded initial request, presence of a null value)")
        void fromItemsUnboundedWithNull() {
            AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
            ZeroPublisher.fromItems(1, null, 3).subscribe(sub);

            sub.assertFailedWith(NullPointerException.class, "null value");
        }
    }

    @Nested
    @DisplayName("Publisher from CompletionStage")
    class CompletionStages {

        @Test
        @DisplayName("Null CompletionStage")
        void fromNull() {
            Assertions.assertThrows(NullPointerException.class, () -> ZeroPublisher.fromCompletionStage(null));
        }

        @Test
        @DisplayName("Resolved CompletionStage (value)")
        void fromResolvedValue() {
            CompletableFuture<Integer> future = CompletableFuture.completedFuture(58);
            AssertSubscriber<Object> sub = AssertSubscriber.create(10);
            ZeroPublisher.fromCompletionStage(future).subscribe(sub);

            sub.assertCompleted();
            sub.assertItems(58);
        }

        @Test
        @DisplayName("Resolved CompletionStage (error)")
        void fromResolvedFailure() {
            CompletableFuture<Integer> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("boom"));
            AssertSubscriber<Object> sub = AssertSubscriber.create(10);
            ZeroPublisher.fromCompletionStage(future).subscribe(sub);

            sub.assertFailedWith(RuntimeException.class, "boom");
        }

        @Test
        @DisplayName("Deferred CompletionStage (value)")
        void fromDeferredValue() {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 63);
            AssertSubscriber<Object> sub = AssertSubscriber.create(10);
            ZeroPublisher.fromCompletionStage(future).subscribe(sub);

            sub.awaitCompletion(Duration.ofSeconds(5));
            sub.assertItems(63).assertCompleted();
        }

        @Test
        @DisplayName("Resolved CompletionStage (null value)")
        void fromResolvedNullValue() {
            CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
            AssertSubscriber<Object> sub = AssertSubscriber.create(10);
            ZeroPublisher.fromCompletionStage(future).subscribe(sub);

            sub.assertFailedWith(NullPointerException.class, "null value");
        }

        @Test
        @DisplayName("Deferred CompletionStage (null value)")
        void fromDeferredNullValue() {
            CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> null);
            AssertSubscriber<Object> sub = AssertSubscriber.create(10);
            ZeroPublisher.fromCompletionStage(future).subscribe(sub);

            sub.awaitFailure(Duration.ofSeconds(5));
            sub.assertFailedWith(NullPointerException.class, "null value");
        }

        @Test
        @DisplayName("Publisher to CompletionStage (value)")
        void publisherToCompletionStageOk() {
            AtomicInteger counter = new AtomicInteger();
            Multi<Integer> publisher = Multi.createFrom()
                    .range(58, 69)
                    .onItem().invoke(counter::incrementAndGet);

            ZeroPublisher.toCompletionStage(publisher)
                    .whenComplete((n, throwable) -> {
                        assertThat(n).isPresent().isEqualTo(58);
                        assertThat(throwable).isNull();
                        assertThat(counter).hasValue(1);
                    });
        }

        @Test
        @DisplayName("Publisher to CompletionStage (empty)")
        void publisherToCompletionStageOkEmpty() {
            AtomicInteger counter = new AtomicInteger();
            Multi<Object> publisher = Multi.createFrom().empty()
                    .onItem().invoke(counter::incrementAndGet);

            ZeroPublisher.toCompletionStage(publisher)
                    .whenComplete((n, throwable) -> {
                        assertThat(n).isNotNull().isNotPresent();
                        assertThat(throwable).isNull();
                        assertThat(counter).hasValue(1);
                    });
        }

        @Test
        @DisplayName("Publisher to CompletionStage (error)")
        void publisherToCompletionStageKo() {
            AtomicInteger counter = new AtomicInteger();
            Multi<Object> publisher = Multi.createFrom()
                    .failure(new IOException("boom"))
                    .onItem().invoke(counter::incrementAndGet);

            ZeroPublisher.toCompletionStage(publisher)
                    .whenComplete((obj, throwable) -> {
                        assertThat(obj).isNull();
                        assertThat(throwable).isInstanceOf(IOException.class).hasMessage("boom");
                        assertThat(counter).hasValue(0);
                    });
        }
    }

    @Nested
    @DisplayName("Publisher from streams")
    class Streams {

        @Test
        @DisplayName("Items from a null stream")
        void fromNull() {
            Assertions.assertThrows(NullPointerException.class, () -> ZeroPublisher.fromStream(null));

            AssertSubscriber<Object> sub = AssertSubscriber.create(10L);
            ZeroPublisher.fromStream(() -> null).subscribe(sub);
            sub.assertFailedWith(NullPointerException.class, "cannot be null");
        }

        @Test
        @DisplayName("Items from a stream (unbounded)")
        void unbounded() {
            Supplier<Stream<Integer>> supplier = () -> IntStream.range(1, 5).boxed();
            Publisher<Integer> publisher = ZeroPublisher.fromStream(supplier);

            for (int i = 0; i < 3; i++) {
                AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
                publisher.subscribe(sub);
                sub.assertItems(1, 2, 3, 4);
                sub.assertCompleted();
            }
        }

        @Test
        @DisplayName("Items from a stream (request batches)")
        void batches() {
            AssertSubscriber<Object> sub = AssertSubscriber.create();
            Supplier<Stream<Integer>> supplier = () -> IntStream.range(1, 5).boxed();
            ZeroPublisher.fromStream(supplier).subscribe(sub);

            sub.request(2L);
            sub.assertItems(1, 2);
            sub.request(1L);
            sub.assertItems(1, 2, 3);
            sub.request(Long.MAX_VALUE);
            sub.assertItems(1, 2, 3, 4);
        }

        @Test
        @DisplayName("Items from a stream (cancellation)")
        void cancellation() {
            AssertSubscriber<Object> sub = AssertSubscriber.create();
            Supplier<Stream<Integer>> supplier = () -> IntStream.range(1, 5).boxed();
            ZeroPublisher.fromStream(supplier).subscribe(sub);

            sub.request(2L);
            sub.assertItems(1, 2);
            sub.cancel();
            sub.assertItems(1, 2);
            sub.assertNotTerminated();
        }
    }

    @Nested
    @DisplayName("Publisher from generator")
    class Generators {

        @Test
        @DisplayName("Null values")
        void fromNull() {
            Iterator<String> infiniteYolo = new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public String next() {
                    return "yolo";
                }
            };

            Assertions.assertThrows(NullPointerException.class, () -> ZeroPublisher.fromGenerator(null, null));
            Assertions.assertThrows(NullPointerException.class, () -> ZeroPublisher.fromGenerator(() -> "yolo", null));
            Assertions.assertThrows(NullPointerException.class, () -> ZeroPublisher.fromGenerator(null, s -> infiniteYolo));

            AssertSubscriber<String> sub = AssertSubscriber.create(3);
            ZeroPublisher.fromGenerator(() -> null, s -> infiniteYolo).subscribe(sub);
            sub.assertItems("yolo", "yolo", "yolo");

            sub = AssertSubscriber.create(3);
            ZeroPublisher.<String, String> fromGenerator(() -> "yolo", s -> null).subscribe(sub);
            sub.assertFailedWith(IllegalArgumentException.class, "null iterator");
        }

        @Test
        @DisplayName("Sequence of naturally increasing integers")
        void integers() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create();
            ZeroPublisher.fromGenerator(() -> 5, max -> new Iterator<Integer>() {
                int current = 0;

                @Override
                public boolean hasNext() {
                    return current < max;
                }

                @Override
                public Integer next() {
                    return current++;
                }
            }).subscribe(sub);

            sub.request(10);
            sub.assertCompleted().assertItems(0, 1, 2, 3, 4);
        }

        @Test
        @DisplayName("Sequence of naturally increasing integers failing at the second request")
        void failingIntegers() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create();
            ZeroPublisher.fromGenerator(() -> null, max -> new Iterator<Integer>() {
                int counter;

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Integer next() {
                    if (counter == 1) {
                        throw new RuntimeException("boom");
                    } else {
                        return counter++;
                    }
                }
            }).subscribe(sub);

            sub.request(10);
            sub.assertFailedWith(RuntimeException.class, "boom");
        }

        @Test
        @DisplayName("Sequence of naturally increasing integers with a null value at the second request")
        void nullInStream() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create();
            ZeroPublisher.fromGenerator(() -> null, max -> new Iterator<Integer>() {
                int counter;

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Integer next() {
                    if (counter == 1) {
                        return null;
                    } else {
                        return counter++;
                    }
                }
            }).subscribe(sub);

            sub.request(10);
            sub.assertFailedWith(NullPointerException.class, "null value");
        }

        @Test
        @DisplayName("Already completed Publisher from a generator")
        void alreadyCompleted() {
            AssertSubscriber<String> sub = AssertSubscriber.create();
            ZeroPublisher.fromGenerator(() -> null, s -> new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public String next() {
                    return "123";
                }
            }).subscribe(sub);

            sub.assertCompleted().assertItems();
        }
    }

    @Nested
    @DisplayName("Publisher from failure")
    class Failures {

        @Test
        @DisplayName("Null CompletionStage")
        void fromNull() {
            Assertions.assertThrows(NullPointerException.class, () -> ZeroPublisher.fromFailure(null));
        }

        @Test
        @DisplayName("Failure")
        void failure() {
            AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
            ZeroPublisher.fromFailure(new IOException("boom")).subscribe(sub);

            sub.assertFailedWith(IOException.class, "boom");
        }
    }

    @Nested
    @DisplayName("Empty publisher")
    class Empty {

        @Test
        @DisplayName("Empty Publisher")
        void empty() {
            AssertSubscriber<Object> sub = AssertSubscriber.create();
            ZeroPublisher.empty().subscribe(sub);

            sub.assertItems().assertCompleted();
        }
    }

    @Nested
    @DisplayName("Dropping tube publishers")
    class DroppingTubes {

        @Test
        @DisplayName("Tube dropping on back-pressure")
        void dropping() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(3);
            ZeroPublisher.<Integer> create(BackpressureStrategy.DROP, -1, tube -> {
                for (int i = 1; i < 20; i++) {
                    tube.send(i);
                }
                tube.complete();
            }).subscribe(sub);

            sub.assertCompleted();
            sub.assertItems(1, 2, 3);
        }

        @Test
        @DisplayName("Tube dropping on back-pressure with error")
        void droppingError() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(10);
            ZeroPublisher.<Integer> create(BackpressureStrategy.DROP, -1, tube -> {
                tube.send(1);
                tube.fail(new IOException("boom"));
            }).subscribe(sub);

            sub.assertItems(1);
            sub.assertFailedWith(IOException.class, "boom");
        }

        @Test
        @DisplayName("Tube dropping cancel")
        void droppingCancel() {
            AtomicBoolean step1 = new AtomicBoolean(false);
            AtomicBoolean step2 = new AtomicBoolean(false);

            AtomicLong requested = new AtomicLong();
            AtomicBoolean cancelled = new AtomicBoolean();
            AtomicBoolean terminated = new AtomicBoolean();

            AssertSubscriber<Integer> sub = AssertSubscriber.create(3);
            ZeroPublisher.<Integer> create(BackpressureStrategy.DROP, -1, tube -> {
                new Thread(() -> {
                    step1.set(true);
                    tube.whenRequested(requested::addAndGet);
                    tube.whenCancelled(() -> cancelled.set(true));
                    tube.whenTerminates(() -> terminated.set(true));
                    await().untilTrue(step2);
                    for (int i = 1; i < 20; i++) {
                        tube.send(i);
                    }
                    tube.complete();
                }).start();
            }).subscribe(sub);

            await().untilTrue(step1);
            sub.cancel();
            step2.set(true);
            sub.assertHasNotReceivedAnyItem().assertNotTerminated();

            assertThat(requested).hasValue(3L);
            assertThat(cancelled).isTrue();
            assertThat(terminated).isTrue();
        }

        @Test
        @DisplayName("Tube dropping on back-pressure (pause then refill)")
        void droppingPauseInMiddle() {
            AtomicBoolean step1 = new AtomicBoolean(false);
            AtomicBoolean step2 = new AtomicBoolean(false);
            AtomicBoolean step3 = new AtomicBoolean(false);

            AssertSubscriber<Integer> sub = AssertSubscriber.create(3);
            ZeroPublisher.<Integer> create(BackpressureStrategy.DROP, -1, tube -> {
                new Thread(() -> {
                    for (int i = 1; i < 10; i++) {
                        tube.send(i);
                    }
                    step1.set(true);
                    await().untilTrue(step2);
                    for (int i = 10; i < 20; i++) {
                        tube.send(i);
                    }
                    tube.complete();
                    step3.set(true);
                }).start();
            }).subscribe(sub);

            await().untilTrue(step1);
            sub.assertItems(1, 2, 3);
            sub.request(3);
            step2.set(true);
            await().untilTrue(step3);
            sub.assertItems(1, 2, 3, 10, 11, 12);
            sub.assertCompleted();
        }
    }

    @Nested
    @DisplayName("Back-pressure erroring tube publishers")
    class ErrorTubes {

        @Test
        @DisplayName("Tube error after 3 items")
        void failAfter3() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(3);
            ZeroPublisher.<Integer> create(BackpressureStrategy.ERROR, -1, tube -> {
                for (int i = 1; i < 20; i++) {
                    tube.send(i);
                }
                tube.complete();
            }).subscribe(sub);

            sub.assertItems(1, 2, 3);
            sub.assertFailedWith(IllegalStateException.class, "there is no demand");
        }

        @Test
        @DisplayName("Tube that don't fail")
        void dontFail() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
            ZeroPublisher.<Integer> create(BackpressureStrategy.ERROR, -1, tube -> {
                for (int i = 1; i < 4; i++) {
                    tube.send(i);
                }
                tube.complete();
            }).subscribe(sub);

            sub.assertItems(1, 2, 3);
            sub.assertCompleted();
        }
    }

    @Nested
    @DisplayName("Back-pressure ignoring tube publishers")
    class IgnoringTubes {

        @Test
        @DisplayName("Tube that doesn't care")
        void yolo() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(3);
            ZeroPublisher.<Integer> create(BackpressureStrategy.IGNORE, -1, tube -> {
                for (int i = 1; i < 6; i++) {
                    tube.send(i);
                }
                tube.complete();
            }).subscribe(sub);

            sub.assertItems(1, 2, 3, 4, 5);
            sub.assertCompleted();
        }

        @Test
        @DisplayName("Tube that completes twice")
        void completeTwice() {
            Assertions.assertThrows(IllegalStateException.class, () -> {
                AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
                ZeroPublisher.<Integer> create(BackpressureStrategy.IGNORE, -1, tube -> {
                    for (int i = 1; i < 6; i++) {
                        tube.send(i);
                    }
                    tube.complete();
                    tube.complete();
                }).subscribe(sub);
            }, "Already completed");
        }

        @Test
        @DisplayName("Send a null")
        void sendNull() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(3);
            ZeroPublisher.<Integer> create(BackpressureStrategy.IGNORE, -1, tube -> {
                for (int i = 1; i < 6; i++) {
                    if (i == 3) {
                        tube.send(null);
                    } else {
                        tube.send(i);
                    }
                }
                tube.complete();
            }).subscribe(sub);

            sub.assertItems(1, 2);
            sub.assertFailedWith(NullPointerException.class, "item is null");
        }

        @Test
        @DisplayName("Cancel in the middle")
        void cancelInTheMiddle() {
            AtomicBoolean step1 = new AtomicBoolean();
            AtomicBoolean step2 = new AtomicBoolean();

            AssertSubscriber<Integer> sub = AssertSubscriber.create(10L);
            ZeroPublisher.<Integer> create(BackpressureStrategy.IGNORE, -1, tube -> {
                new Thread(() -> {
                    for (int i = 1; i < 20; i++) {
                        tube.send(i);
                        if (i == 5) {
                            step1.set(true);
                            await().untilTrue(step2);
                        }
                    }
                    tube.complete();
                }).start();
            }).subscribe(sub);

            await().untilTrue(step1);
            sub.cancel();
            sub.request(150L);
            sub.cancel();
            step2.set(true);

            sub.assertNotTerminated();
            sub.assertItems(1, 2, 3, 4, 5);
        }
    }

    @Nested
    @DisplayName("Buffering tube publishers")
    class BufferingTubes {

        @Test
        @DisplayName("Bad initialization parameters")
        void badInit() {
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> ZeroPublisher.create(BackpressureStrategy.BUFFER, -1, tube -> {
                        // Nothing here
                    }));

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> ZeroPublisher.create(BackpressureStrategy.BUFFER, 0, tube -> {
                        // Nothing here
                    }));
        }

        @Test
        @DisplayName("No overflow")
        void noOverflow() {
            AtomicLong requested = new AtomicLong();
            AtomicBoolean cancelled = new AtomicBoolean();
            AtomicBoolean terminated = new AtomicBoolean();

            AssertSubscriber<Integer> sub = AssertSubscriber.create(10L);
            ZeroPublisher.<Integer> create(BackpressureStrategy.BUFFER, 256, tube -> {
                tube.whenRequested(requested::addAndGet)
                        .whenTerminates(() -> terminated.set(true))
                        .whenCancelled(() -> cancelled.set(true));
                for (int i = 1; i < 6; i++) {
                    tube.send(i);
                }
                tube.complete();
            }).subscribe(sub);

            sub.assertItems(1, 2, 3, 4, 5);
            sub.assertCompleted();

            assertThat(cancelled).isFalse();
            assertThat(terminated).isTrue();
            assertThat(requested).hasValue(10L);
        }

        @Test
        @DisplayName("Overflow within the buffer bounds")
        void overflowOk() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(5);
            ZeroPublisher.<Integer> create(BackpressureStrategy.BUFFER, 256, tube -> {
                for (int i = 1; i < 100; i++) {
                    tube.send(i);
                }
                tube.complete();
            }).subscribe(sub);

            sub.assertItems(1, 2, 3, 4, 5);
            sub.assertNotTerminated();
        }

        @Test
        @DisplayName("Overflow outside the buffer bounds")
        void overflowKo() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(5);
            ZeroPublisher.<Integer> create(BackpressureStrategy.BUFFER, 256, tube -> {
                for (int i = 1; i < 512; i++) {
                    tube.send(i);
                }
                tube.complete();
            }).subscribe(sub);

            sub.assertItems(1, 2, 3, 4, 5);
            sub.assertFailedWith(IllegalStateException.class, "there is no demand and the overflow buffer is full: 262");
        }

        @Test
        @DisplayName("Overflow then drain")
        void overflowThenDrain() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(5);
            ZeroPublisher.<Integer> create(BackpressureStrategy.BUFFER, 10, tube -> {
                for (int i = 1; i < 11; i++) {
                    tube.send(i);
                }
                tube.complete();
            }).subscribe(sub);

            sub.assertItems(1, 2, 3, 4, 5);
            sub.assertNotTerminated();

            sub.request(100L);
            sub.assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            sub.assertCompleted();
        }

        @Test
        @DisplayName("Overflow then drain then refill")
        void overflowThenDrainThenRefill() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create();
            AtomicBoolean step0 = new AtomicBoolean();
            AtomicBoolean step1 = new AtomicBoolean();
            AtomicBoolean step2 = new AtomicBoolean();

            ZeroPublisher.<Integer> create(BackpressureStrategy.BUFFER, 256, tube -> {
                new Thread(() -> {
                    for (int i = 1; i < 20; i++) {
                        tube.send(i);
                    }
                    step0.set(true);
                    await().untilTrue(step1);
                    for (int i = 1; i < 20; i++) {
                        tube.send(i);
                    }
                    tube.complete();
                    step2.set(true);
                }).start();
            }).subscribe(sub);

            await().untilTrue(step0);
            sub.request(10L);

            sub.assertNotTerminated();
            sub.assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

            step1.set(true);
            sub.request(5L);
            await().untilTrue(step2);
            sub.request(100L);

            sub.assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                    11, 12, 13, 14, 15, 16, 17, 18, 19);
            sub.assertCompleted();
        }

        @Test
        @DisplayName("Unbounded buffer")
        void unbounded() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(5);

            ZeroPublisher.<Integer> create(BackpressureStrategy.UNBOUNDED_BUFFER, -1, tube -> {
                for (int i = 1; i < 101; i++) {
                    tube.send(i);
                }
                tube.complete();
            }).subscribe(sub);

            sub.assertItems(1, 2, 3, 4, 5);
            sub.assertNotTerminated();

            sub.request(50);
            sub.assertNotTerminated();
            assertThat(sub.getItems()).hasSize(55).endsWith(53, 54, 55);

            sub.request(Long.MAX_VALUE);
            sub.assertCompleted();
            assertThat(sub.getItems()).hasSize(100).endsWith(98, 99, 100);
        }
    }

    @Nested
    @DisplayName("Latest tube publishers")
    class LatestTubes {

        @Test
        @DisplayName("Bad initialization parameters")
        void badInit() {
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> ZeroPublisher.create(BackpressureStrategy.LATEST, -1, tube -> {
                        // Nothing here
                    }));

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> ZeroPublisher.create(BackpressureStrategy.LATEST, 0, tube -> {
                        // Nothing here
                    }));
        }

        @Test
        @DisplayName("Bad request")
        void badReq() {
            Publisher<Object> publisher = ZeroPublisher.create(BackpressureStrategy.DROP, -1, tube -> {
                // Nothing here
            });

            AssertSubscriber<Object> sub = AssertSubscriber.create();
            publisher.subscribe(sub);
            sub.request(0L);
            sub.assertFailedWith(IllegalArgumentException.class, "must be > 0L");

            sub = AssertSubscriber.create();
            publisher.subscribe(sub);
            sub.request(-100L);
            sub.assertFailedWith(IllegalArgumentException.class, "must be > 0L");

        }

        @Test
        @DisplayName("No overflow")
        void noOverflow() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(10);
            ZeroPublisher.<Integer> create(BackpressureStrategy.LATEST, 256, tube -> {
                for (int i = 1; i < 6; i++) {
                    tube.send(i);
                }
                tube.complete();
            }).subscribe(sub);

            sub.assertItems(1, 2, 3, 4, 5);
            sub.assertCompleted();
        }

        @Test
        @DisplayName("Overflow within the buffer bounds")
        void overflowWithin() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(5);
            ZeroPublisher.<Integer> create(BackpressureStrategy.LATEST, 256, tube -> {
                for (int i = 1; i < 100; i++) {
                    tube.send(i);
                }
                tube.complete();
            }).subscribe(sub);

            sub.assertItems(1, 2, 3, 4, 5);
            sub.assertNotTerminated();
        }

        @Test
        @DisplayName("Overflow outside the buffer bounds")
        void overflowOver() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create();
            ZeroPublisher.<Integer> create(BackpressureStrategy.LATEST, 5, tube -> {
                for (int i = 1; i < 500; i++) {
                    tube.send(i);
                }
                tube.complete();
            }).subscribe(sub);

            sub.request(Long.MAX_VALUE);
            sub.assertItems(495, 496, 497, 498, 499);
            sub.assertCompleted();
        }
    }
}
