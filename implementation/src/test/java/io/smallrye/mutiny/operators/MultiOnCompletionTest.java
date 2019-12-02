package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import io.reactivex.Flowable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiOnCompletionTest {

    @Test
    public void testOnCompletionContinueWith() {
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().range(1, 5)
                .onCompletion().consume(() -> called.set(true))
                .onCompletion().continueWith(6, 7, 8)
                .subscribe().with(MultiAssertSubscriber.create(7))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 6, 7, 8);

        assertThat(called).isTrue();
    }

    @Test
    public void testOnCompletionContinueWithAndUpstreamFailure() {
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().emitter(e -> e.emit(1).emit(2).fail(new IOException("boom")))
                .onCompletion().consume(() -> called.set(true))
                .onCompletion().continueWith(6, 7, 8)
                .subscribe().with(MultiAssertSubscriber.create(7))
                .assertHasFailedWith(IOException.class, "boom")
                .assertReceived(1, 2);

        assertThat(called).isFalse();
    }

    @Test
    public void testOnCompletionContinueWithEmpty() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith()
                .subscribe().with(MultiAssertSubscriber.create(7))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testOnCompletionContinueWithOne() {
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().range(1, 5)
                .onCompletion().consume(() -> called.set(true))
                .onCompletion().continueWith(25)
                .subscribe().with(MultiAssertSubscriber.create(7))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 25);

        assertThat(called).isTrue();
    }

    @Test
    public void testOnCompletionContinueWithIterable() {
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().range(1, 5)
                .onCompletion().consume(() -> called.set(true))
                .onCompletion().continueWith(Arrays.asList(5, 6))
                .subscribe().with(MultiAssertSubscriber.create(7))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6);

        assertThat(called).isTrue();
    }

    @Test
    public void testOnCompletionContinueWithEmptyIterable() {
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().range(1, 5)
                .onCompletion().consume(() -> called.set(true))
                .onCompletion().continueWith(Collections.emptyList())
                .subscribe().with(MultiAssertSubscriber.create(7))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);

        assertThat(called).isTrue();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testOnCompletionContinueWithNullItem() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith((Integer) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testOnCompletionContinueWithNullAsIterable() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith((Iterable<Integer>) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testOnCompletionContinueWithItemsContainingNullItem() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith(1, 2, 3, null, 4, 5);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testOnCompletionContinueWithIterableContainingNullItem() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith(Arrays.asList(1, 2, 3, null, 4, 5));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testOnCompletionContinueWithNullSupplier() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith((Supplier<? extends Iterable<? extends Integer>>) null);
    }

    @Test
    public void testOnCompletionContinueWithSupplier() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith(() -> Arrays.asList(25, 26))
                .subscribe().with(MultiAssertSubscriber.create(20))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 25, 26);
    }

    @Test
    public void testOnCompletionContinueWithSupplierReturningEmpty() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith((Supplier<Iterable<? extends Integer>>) Collections::emptyList)
                .subscribe().with(MultiAssertSubscriber.create(20))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testOnCompletionContinueWithSupplierContainingNullItem() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith(() -> Arrays.asList(25, null, 26))
                .subscribe().with(MultiAssertSubscriber.create(20))
                .assertHasFailedWith(NullPointerException.class, null)
                .assertReceived(1, 2, 3, 4, 25);
    }

    @Test
    public void testOnCompletionContinueWithSupplierReturningNull() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith(() -> (Iterable<Integer>) null)
                .subscribe().with(MultiAssertSubscriber.create(20))
                .assertHasFailedWith(NullPointerException.class, null)
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testOnCompletionContinueWithSupplierThrowingException() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith((Supplier<? extends Iterable<? extends Integer>>) () -> {
                    throw new IllegalStateException("BOOM!");
                })
                .subscribe().with(MultiAssertSubscriber.create(20))
                .assertHasFailedWith(IllegalStateException.class, "BOOM!")
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testOnCompletionFail() {
        Multi.createFrom().range(1, 5)
                .onCompletion().fail()
                .subscribe().with(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertReceived(1, 2, 3, 4)
                .assertHasFailedWith(NoSuchElementException.class, null);
    }

    @Test
    public void testOnCompletionFailWithException() {
        Multi.createFrom().range(1, 5)
                .onCompletion().failWith(new IOException("boom"))
                .subscribe().with(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertReceived(1, 2, 3, 4)
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testOnCompletionFailWithNullException() {
        Multi.createFrom().range(1, 5)
                .onCompletion().failWith((Throwable) null);

    }

    @Test
    public void testOnCompletionFailWithSupplier() {
        Multi.createFrom().range(1, 5)
                .onCompletion().failWith(() -> new IOException("boom"))
                .subscribe().with(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertReceived(1, 2, 3, 4)
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testOnCompletionFailWithNullSupplier() {
        Multi.createFrom().range(1, 5)
                .onCompletion().failWith((Supplier<Throwable>) null);
    }

    @Test
    public void testOnCompletionFailWithSupplierThrowingException() {
        Multi.createFrom().range(1, 5)
                .onCompletion().failWith(() -> {
                    throw new IllegalStateException("BOOM!");
                })
                .subscribe().with(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertReceived(1, 2, 3, 4)
                .assertHasFailedWith(IllegalStateException.class, "BOOM!");
    }

    @Test
    public void testOnCompletionFailWithSupplierReturningNull() {
        Multi.createFrom().range(1, 5)
                .onCompletion().failWith(() -> null)
                .subscribe().with(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .assertReceived(1, 2, 3, 4)
                .assertHasFailedWith(NullPointerException.class, null);
    }

    @Test
    public void testSwitchTo() {
        Multi.createFrom().range(1, 5)
                .onCompletion().switchTo(Flowable.just(20))
                .subscribe().with(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 20);
    }

    @Test
    public void testSwitchToSupplier() {
        Multi.createFrom().range(1, 5)
                .onCompletion().switchTo(() -> Multi.createFrom().range(5, 8))
                .subscribe().with(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void testSwitchToSupplierReturningNull() {
        Multi.createFrom().range(1, 5)
                .onCompletion().switchTo(() -> null)
                .subscribe().with(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(NullPointerException.class, null)
                .assertReceived(1, 2, 3, 4);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSwitchToWithConsumerBeingNull() {
        Multi.createFrom().range(1, 5)
                .onCompletion().switchToEmitter(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSwitchToWithSupplierBeingNull() {
        Multi.createFrom().range(1, 5)
                .onCompletion().switchTo((Supplier<Publisher<? extends Integer>>) null);
    }

    @Test
    public void testSwitchToWithConsumer() {
        Multi.createFrom().range(1, 5)
                .onCompletion().switchToEmitter(e -> e.emit(5).emit(6).complete())
                .subscribe().with(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6);
    }

}
