package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCreateFromItemsTest {

    @Test
    public void testCreationWithASingleResult() {
        Multi<Integer> multi = Multi.createFrom().item(1);
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .request(1)
                .assertCompletedSuccessfully()
                .assertReceived(1);
    }

    @Test
    public void testCreationWithASingleNullResult() {
        Multi<String> multi = Multi.createFrom().item((String) null);
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testCreationWithASingleResultProducedBySupplier() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().item(count::incrementAndGet);
        assertThat(count).hasValue(0);
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .run(() -> assertThat(count).hasValue(1)) // The supplier is called at subscription time
                .request(1)
                .assertCompletedSuccessfully()
                .assertReceived(1);

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .request(1)
                .assertCompletedSuccessfully()
                .assertReceived(2);
    }

    @Test
    public void testCreationWithNullProducedBySupplier() {
        Multi<Integer> multi = Multi.createFrom().item(() -> null);
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testCreationWithExceptionThrownBySupplier() {
        Multi<Integer> multi = Multi.createFrom().item(() -> {
            throw new IllegalStateException("boom");
        });
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testCreationFromAStreamWithRequest() {
        Multi<Integer> multi = Multi.createFrom().items(Stream.of(1, 2, 3));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .request(1)
                .assertReceived(1)
                .request(3)
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();

        AtomicInteger count = new AtomicInteger();
        multi = Multi.createFrom().items(() -> {
            count.incrementAndGet();
            return Stream.of(1, 2, 3);
        });
        assertThat(count).hasValue(0);
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .run(() -> assertThat(count).hasValue(1))
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .request(1)
                .assertReceived(1)
                .request(3)
                .assertReceived(1, 2, 3)
                .run(() -> assertThat(count).hasValue(1))
                .assertCompletedSuccessfully();
    }

    @Test
    public void testCreateFromEmptyStream() {
        Multi.createFrom().<Integer> items(Stream.empty())
                .subscribe().withSubscriber(MultiAssertSubscriber.create(100))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();

        Multi.createFrom().<Integer> items(Stream::empty)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(100))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreateFromStreamOfOne() {
        Multi.createFrom().items(Stream.of(1))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(100))
                .assertCompletedSuccessfully()
                .assertReceived(1);

        Multi.createFrom().items(() -> Stream.of(2))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(100))
                .assertCompletedSuccessfully()
                .assertReceived(2);
    }

    @Test
    public void testThatStreamCannotBeReused() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4);
        List<Integer> list = Multi.createFrom().items(stream)
                .collectItems().asList()
                .await().indefinitely();

        assertThat(list).containsExactly(1, 2, 3, 4);

        assertThatThrownBy(() -> Multi.createFrom().items(stream)
                .collectItems().asList()
                .await().indefinitely()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testThatMultiBasedOnStreamCannotBeReused() {
        Stream<Integer> stream = Stream.of(1, 2, 3, 4);
        Multi<Integer> multi = Multi.createFrom().items(stream);
        List<Integer> list = multi
                .collectItems().asList()
                .await().indefinitely();

        assertThat(list).containsExactly(1, 2, 3, 4);

        assertThatThrownBy(() -> multi
                .collectItems().asList()
                .await().indefinitely()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testLimitOnMultiBasedOnStream() {
        Multi.createFrom().items(() -> IntStream.iterate(0, operand -> operand + 1).boxed())
                .transform().byTakingFirstItems(10)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertCompletedSuccessfully()
                .assertReceived(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testNullWithStreams() {
        assertThatThrownBy(() -> Multi.createFrom().items((Stream<String>) null)).isInstanceOf(IllegalArgumentException.class);

        Multi.createFrom().items(() -> null)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(NullPointerException.class, "")
                .assertHasNotReceivedAnyItem();

        Multi.createFrom().items(Stream.of("a", "b", null, "c"))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(NullPointerException.class, "")
                .assertReceived("a", "b");
    }

    @Test
    public void testCloseCallbackCalledWithStream() {
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().items(Stream.of("a", "b", "c").onClose(() -> called.set(true)))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertReceived("a", "b", "c")
                .assertCompletedSuccessfully()
                .run(() -> assertThat(called).isTrue());

        // Test that the callback is called when the subscriber cancels
        called.set(false);
        Multi.createFrom().items(Stream.of("a", "b", "c").onClose(() -> called.set(true)))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertReceived("a")
                .cancel()
                .run(() -> assertThat(called).isTrue());
    }

    @Test
    public void testStreamHasNextFailureWithStream() {
        AtomicInteger counter = new AtomicInteger();
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().items(Stream.generate(() -> {
            int value = counter.getAndIncrement();
            if (value == 1) {
                throw new IllegalStateException("boom");
            }
            return value;
        }).onClose(() -> called.set(true))).subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IllegalStateException.class, "boom")
                .assertReceived(0);
        assertThat(called).isTrue();
    }

    @Test
    public void testCreationFromAStreamSupplier() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().items(() -> Stream.of(1, 2, count.incrementAndGet()));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .request(1)
                .assertReceived(1)
                .request(3)
                .assertReceived(1, 2, 1)
                .assertCompletedSuccessfully();

        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .request(1)
                .assertReceived(1)
                .request(3)
                .assertReceived(1, 2, 2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testCreationFromAnEmptyStream() {
        Multi<Integer> multi = Multi.createFrom().items(Stream.of());
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testCreationFromAnEmptyStreamSupplier() {
        Multi<Integer> multi = Multi.createFrom().items(Stream::empty);
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .assertCompletedSuccessfully();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreationFromANullStream() {
        Multi.createFrom().items((Stream<Integer>) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreationFromANullStreamSupplier() {
        Multi.createFrom().items((Supplier<Stream<Integer>>) null);
    }

    @Test
    public void testCreationFromAStreamSupplierProducingNull() {
        Multi<Integer> multi = Multi.createFrom().items((Supplier<Stream<Integer>>) () -> null);
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasFailedWith(NullPointerException.class, "supplier");
    }

    @Test
    public void testCreationFromAStreamSupplierThrowingAnException() {
        Multi<Integer> multi = Multi.createFrom().items((Supplier<Stream<Integer>>) () -> {
            throw new IllegalStateException("boom");
        });
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testCreationFromResults() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .request(2)
                .assertReceived(1, 2)
                .request(1)
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreationFromResultsContainingNull() {
        Multi.createFrom().items(1, null, 3);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreationFromResultsWithNull() {
        Multi.createFrom().items((Integer[]) null);
    }

    @Test
    public void testCreationFromIterable() {
        Multi<Integer> multi = Multi.createFrom().iterable(Arrays.asList(1, 2, 3));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .request(2)
                .assertReceived(1, 2)
                .request(1)
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreationFromIterableWithNull() {
        Multi.createFrom().iterable((Iterable<Integer>) null);
    }

    @Test
    public void testCreationFromIterableContainingNull() {
        Multi<Integer> multi = Multi.createFrom().iterable(Arrays.asList(1, null, 3));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .request(2)
                .assertReceived(1)
                .assertHasFailedWith(NullPointerException.class, "");
    }

}
