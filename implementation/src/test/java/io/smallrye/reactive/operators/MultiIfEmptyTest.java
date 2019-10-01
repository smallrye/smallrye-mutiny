package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;
import org.junit.Test;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiIfEmptyTest {

    @Test
    public void testIfEmptyContinueWith() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(6, 7, 8)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(7))
                .assertCompletedSuccessfully()
                .assertReceived(6, 7, 8);
        
    }

    @Test
    public void testIfEmptyContinueWithOnNonEmpty() {
        Multi.createFrom().item(1)
                .onCompletion().ifEmpty().continueWith(6, 7, 8)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(7))
                .assertCompletedSuccessfully()
                .assertReceived(1);

    }

    @Test
    public void testIfEmptyContinueWithAndUpstreamFailure() {
        Multi.createFrom().<Integer>failure(new IOException("boom"))
                .onCompletion().ifEmpty().continueWith(6, 7, 8)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(7))
                .assertHasFailedWith(IOException.class, "boom");

    }

    @Test
    public void testIfEmptyContinueWithEmpty() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(7))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testIfEmptyContinueWithOne() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(25)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(7))
                .assertCompletedSuccessfully()
                .assertReceived(25);
    }

    @Test
    public void testIfEmptyBecauseOfSkipContinueWithOne() {
        Multi.createFrom().items(1, 2, 3).transform().bySkippingFirstItems(5)
                .onCompletion().ifEmpty().continueWith(25)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(7))
                .assertCompletedSuccessfully()
                .assertReceived(25);
    }

    @Test
    public void testIfEmptyContinueWithIterable() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(Arrays.asList(5, 6))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(7))
                .assertCompletedSuccessfully()
                .assertReceived(5, 6);
    }

    @Test
    public void testIfEmptyContinueWithEmptyIterable() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(Collections.emptyList())
                .subscribe().withSubscriber(MultiAssertSubscriber.create(7))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIfEmptyContinueWithNullItem() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith((Integer) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIfEmptyContinueWithNullAsIterable() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith((Iterable<Integer>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIfEmptyContinueWithItemsContainingNullItem() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(1, 2, 3, null, 4, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIfEmptyContinueWithIterableContainingNullItem() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(Arrays.asList(1, 2, 3, null, 4, 5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIfEmptyContinueWithNullSupplier() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith((Supplier<? extends Iterable<? extends Integer>>) null);
    }

    @Test
    public void testIfEmptyContinueWithSupplier() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(() -> Arrays.asList(25, 26))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(20))
                .assertCompletedSuccessfully()
                .assertReceived(25, 26);
    }

    @Test
    public void testIfEmptyContinueWithSupplierReturningEmpty() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith((Supplier<Iterable<? extends Integer>>) Collections::emptyList)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(20))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testIfEmptyContinueWithSupplierContainingNullItem() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(() -> Arrays.asList(25, null, 26))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(20))
                .assertHasFailedWith(IllegalArgumentException.class, null)
                .assertReceived(25);
    }

    @Test
    public void testIfEmptyContinueWithSupplierReturningNull() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith(() -> (Iterable<Integer>) null)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(20))
                .assertHasFailedWith(NullPointerException.class, null);
    }

    @Test
    public void testIfEmptyContinueWithSupplierThrowingException() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().continueWith((Supplier<? extends Iterable<? extends Integer>>) () -> {
            throw new IllegalStateException("BOOM!");
        })
                .subscribe().withSubscriber(MultiAssertSubscriber.create(20))
                .assertHasFailedWith(IllegalStateException.class, "BOOM!")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testIfEmptyFail() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().fail()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertHasFailedWith(NoSuchElementException.class, null)
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testIfEmptyFailWithException() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().failWith(new IOException("boom"))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertHasFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIfEmptyFailWithNullException() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().failWith((Throwable) null);

    }

    @Test
    public void testIfEmptyFailWithSupplier() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().failWith(() -> new IOException("boom"))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertHasFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIfEmptyFailWithNullSupplier() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().failWith((Supplier<Throwable>) null);
    }

    @Test
    public void testIfEmptyFailWithSupplierThrowingException() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().failWith(() -> {
            throw new IllegalStateException("BOOM!");
        })
                .subscribe().withSubscriber(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertHasFailedWith(IllegalStateException.class, "BOOM!");
    }

    @Test
    public void testIfEmptyFailWithSupplierReturningNull() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().failWith(() -> null)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertHasFailedWith(NullPointerException.class, null);
    }

    @Test
    public void testSwitchTo() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().switchTo(Flowable.just(20))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(20);
    }

    @Test
    public void testSwitchToSupplier() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().switchTo(() -> Multi.createFrom().range(5, 8))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(5, 6, 7);
    }

    @Test
    public void testSwitchToSupplierReturningNull() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().switchTo(() -> null)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(NullPointerException.class, null)
                .assertHasNotReceivedAnyItem();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSwitchToWithConsumerBeingNull() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().switchToEmitter(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSwitchToWithSupplierBeingNull() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().switchTo((Supplier<Publisher<?>>) null);
    }

    @Test
    public void testSwitchToWithConsumer() {
        Multi.createFrom().empty()
                .onCompletion().ifEmpty().switchToEmitter(e -> e.emit(5).emit(6).complete())
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(5, 6);
    }

}
