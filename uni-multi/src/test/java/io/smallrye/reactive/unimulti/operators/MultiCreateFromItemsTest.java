package io.smallrye.reactive.unimulti.operators;

import io.smallrye.reactive.unimulti.Multi;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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
        Multi<Integer> multi = Multi.createFrom().deferredItem(count::incrementAndGet);
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
        Multi<Integer> multi = Multi.createFrom().deferredItem(() -> null);
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testCreationWithExceptionThrownBySupplier() {
        Multi<Integer> multi = Multi.createFrom().deferredItem(() -> {
            throw new IllegalStateException("boom");
        });
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testCreationFromAStream() {
        Multi<Integer> multi = Multi.createFrom().items(Stream.of(1, 2, 3));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .request(1)
                .assertReceived(1)
                .request(3)
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testCreationFromAStreamSupplier() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().deferredItems(() -> Stream.of(1, 2, count.incrementAndGet()));
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
        Multi<Integer> multi = Multi.createFrom().deferredItems(Stream::empty);
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .assertCompletedSuccessfully();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreationFromANullStream() {
        Multi.createFrom().items((Stream<Integer>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreationFromANullStreamSupplier() {
        Multi.createFrom().deferredItems((Supplier<Stream<Integer>>) null);
    }

    @Test
    public void testCreationFromAStreamSupplierProducingNull() {
        Multi<Integer> multi = Multi.createFrom().deferredItems((Supplier<Stream<Integer>>) () -> null);
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasFailedWith(NullPointerException.class, "supplier");
    }

    @Test
    public void testCreationFromAStreamSupplierThrowingAnException() {
        Multi<Integer> multi = Multi.createFrom().deferredItems((Supplier<Stream<Integer>>) () -> {
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

    @Test
    public void testCreationFromResultsContainingNull() {
        Multi<Integer> multi = Multi.createFrom().items(1, null, 3);
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasNotReceivedAnyItem()
                .assertSubscribed()
                .request(2)
                .assertReceived(1)
                .assertHasFailedWith(IllegalArgumentException.class, "");
    }

    @Test(expected = IllegalArgumentException.class)
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

    @Test(expected = IllegalArgumentException.class)
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
                .assertHasFailedWith(IllegalArgumentException.class, "");
    }

}
