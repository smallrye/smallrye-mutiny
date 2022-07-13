package io.smallrye.mutiny.operators;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Flowable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.flow.adapters.AdaptersToFlow;

public class MultiIfEmptyTest {

    @Test
    public void testIfEmptyContinueWith() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(6, 7, 8)
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertItems(6, 7, 8);

    }

    @Test
    public void testIfEmptyContinueWithOnNonEmpty() {
        Multi.createFrom().item(1)
                .onCompletion().ifEmpty().continueWith(6, 7, 8)
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertItems(1);

    }

    @Test
    public void testIfEmptyContinueWithAndUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .onCompletion().ifEmpty().continueWith(6, 7, 8)
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertFailedWith(IOException.class, "boom");

    }

    @Test
    public void testIfEmptyContinueWithEmpty() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith()
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testIfEmptyContinueWithOne() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(25)
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertItems(25);
    }

    @Test
    public void testIfEmptyBecauseOfSkipContinueWithOne() {
        Multi.createFrom().items(1, 2, 3)
                .skip().first(5)
                .onCompletion().ifEmpty().continueWith(25)
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertItems(25);
    }

    @Test
    public void testIfEmptyContinueWithIterable() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(Arrays.asList(5, 6))
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertItems(5, 6);
    }

    @Test
    public void testIfEmptyContinueWithEmptyIterable() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(Collections.emptyList())
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testIfEmptyContinueWithNullItem() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith((Integer) null));
    }

    @Test
    public void testIfEmptyContinueWithNullAsIterable() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith((Iterable<Integer>) null));
    }

    @Test
    public void testIfEmptyContinueWithItemsContainingNullItem() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(1, 2, 3, null, 4, 5));
    }

    @Test
    public void testIfEmptyContinueWithIterableContainingNullItem() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(Arrays.asList(1, 2, 3, null, 4, 5)));
    }

    @Test
    public void testIfEmptyContinueWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith((Supplier<? extends Iterable<? extends Integer>>) null));
    }

    @Test
    public void testIfEmptyContinueWithSupplier() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(() -> Arrays.asList(25, 26))
                .subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertCompleted()
                .assertItems(25, 26);
    }

    @Test
    public void testIfEmptyContinueWithSupplierReturningEmpty() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith((Supplier<Iterable<? extends Integer>>) Collections::emptyList)
                .subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testIfEmptyContinueWithSupplierContainingNullItem() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(() -> Arrays.asList(25, null, 26))
                .subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertFailedWith(NullPointerException.class, null)
                .assertItems(25);
    }

    @Test
    public void testIfEmptyContinueWithSupplierReturningNull() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(() -> (Iterable<Integer>) null)
                .subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertFailedWith(NullPointerException.class, null);
    }

    @Test
    public void testIfEmptyContinueWithSupplierThrowingException() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith((Supplier<? extends Iterable<? extends Integer>>) () -> {
                    throw new IllegalStateException("BOOM!");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertFailedWith(IllegalStateException.class, "BOOM!")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testIfEmptyFail() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().fail()
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertFailedWith(NoSuchElementException.class, null)
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testIfEmptyFailWithException() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().failWith(new IOException("boom"))
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testIfEmptyFailWithNullException() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().empty()
                .onCompletion().ifEmpty().failWith((Throwable) null));
    }

    @Test
    public void testIfEmptyFailWithSupplier() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().failWith(() -> new IOException("boom"))
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testIfEmptyFailWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().empty()
                .onCompletion().ifEmpty().failWith((Supplier<Throwable>) null));
    }

    @Test
    public void testIfEmptyFailWithSupplierThrowingException() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().failWith(() -> {
                    throw new IllegalStateException("BOOM!");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertFailedWith(IllegalStateException.class, "BOOM!");
    }

    @Test
    public void testIfEmptyFailWithSupplierReturningNull() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().failWith(() -> null)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertFailedWith(NullPointerException.class, null);
    }

    @Test
    public void testSwitchTo() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().switchTo(AdaptersToFlow.publisher(Flowable.just(20)))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(20);
    }

    @Test
    public void testSwitchToSupplier() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().switchTo(() -> Multi.createFrom().range(5, 8))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(5, 6, 7);
    }

    @Test
    public void testSwitchToSupplierReturningNull() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().switchTo(() -> null)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(NullPointerException.class, null)
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSwitchToWithConsumerBeingNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().empty()
                .onCompletion().ifEmpty().switchToEmitter(null));
    }

    @Test
    public void testSwitchToWithSupplierBeingNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().empty()
                .onCompletion().ifEmpty().switchTo((Supplier<Publisher<?>>) null));
    }

    @Test
    public void testSwitchToWithConsumer() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().switchToEmitter(e -> e.emit(5).emit(6).complete())
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(5, 6);
    }

}
