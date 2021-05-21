package mutiny.zero;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;

class PublisherHelpersTest {

    @Nested
    @DisplayName("Simple map operator")
    class Map {

        @Test
        @DisplayName("Map values")
        void map() {
            AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
            PublisherHelpers.map(ZeroPublisher.fromItems(1, 2, 3), n -> n * 10).subscribe(sub);
            sub.assertCompleted().assertItems(10, 20, 30);
        }

        @Test
        @DisplayName("Mapper fails")
        void mapFail() {
            AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
            PublisherHelpers.map(ZeroPublisher.fromItems(1, 2, 3), n -> {
                throw new RuntimeException("boom");
            }).subscribe(sub);
            sub.assertFailedWith(RuntimeException.class, "boom");
        }

        @Test
        @DisplayName("Null mapper")
        void nullMapper() {
            AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
            Assert.assertThrows(NullPointerException.class,
                    () -> PublisherHelpers.map(ZeroPublisher.fromItems(1, 2, 3), null).subscribe(sub));
        }
    }

    @Nested
    @DisplayName("Collect all items to a list")
    class ToList {

        @Test
        @DisplayName("Collect items")
        void collect() {
            AtomicReference<List<Integer>> items = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            PublisherHelpers
                    .collectToList(ZeroPublisher.fromItems(1, 2, 3))
                    .whenComplete((list, err) -> {
                        items.set(list);
                        error.set(err);
                    });

            assertNotNull(items.get());
            assertIterableEquals(Arrays.asList(1, 2, 3), items.get());
            assertNull(error.get());
        }

        @Test
        @DisplayName("Collect error")
        void collectError() {
            AtomicReference<List<Object>> items = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            PublisherHelpers
                    .collectToList(ZeroPublisher.fromFailure(new IOException("boom")))
                    .whenComplete((list, err) -> {
                        items.set(list);
                        error.set(err);
                    });

            assertNull(items.get());
            assertNotNull(error.get());
            assertTrue(error.get() instanceof IOException);
            assertEquals("boom", error.get().getMessage());
        }
    }
}