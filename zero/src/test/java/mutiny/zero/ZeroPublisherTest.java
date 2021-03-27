package mutiny.zero;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

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
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return 63;
            });
            AssertSubscriber<Object> sub = AssertSubscriber.create(10);
            ZeroPublisher.fromCompletionStage(future).subscribe(sub);

            sub.awaitNextItem();
            sub.assertItems(63);
            sub.assertCompleted();
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
            CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            });
            AssertSubscriber<Object> sub = AssertSubscriber.create(10);
            ZeroPublisher.fromCompletionStage(future).subscribe(sub);

            sub.awaitFailure();
            sub.assertFailedWith(NullPointerException.class, "null value");
        }
    }

    @Nested
    @DisplayName("Publisher from streams")
    class Streams {

        @Test
        @DisplayName("Items from a null stream")
        void fromNull() {
            Assertions.assertThrows(NullPointerException.class, () -> ZeroPublisher.fromStream(null));
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
}
