package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Flowable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.flow.adapters.AdaptersToFlow;

public class MultiOnCompletionTest {

    @Test
    public void testOnCompletionContinueWith() {
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().range(1, 5)
                .onCompletion().invoke(() -> called.set(true))
                .onCompletion().continueWith(6, 7, 8)
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 6, 7, 8);

        assertThat(called).isTrue();
    }

    @Test
    public void testOnCompletionContinueWithAndUpstreamFailure() {
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().emitter(e -> e.emit(1).emit(2).fail(new IOException("boom")))
                .onCompletion().invoke(() -> called.set(true))
                .onCompletion().continueWith(6, 7, 8)
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertFailedWith(IOException.class, "boom")
                .assertItems(1, 2);

        assertThat(called).isFalse();
    }

    @Test
    public void testOnCompletionContinueWithEmpty() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith()
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testOnCompletionContinueWithOne() {
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().range(1, 5)
                .onCompletion().invoke(() -> called.set(true))
                .onCompletion().continueWith(25)
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 25);

        assertThat(called).isTrue();
    }

    @Test
    public void testOnCompletionContinueWithIterable() {
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().range(1, 5)
                .onCompletion().invoke(() -> called.set(true))
                .onCompletion().continueWith(Arrays.asList(5, 6))
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 5, 6);

        assertThat(called).isTrue();
    }

    @Test
    public void testOnCompletionContinueWithEmptyIterable() {
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().range(1, 5)
                .onCompletion().invoke(() -> called.set(true))
                .onCompletion().continueWith(Collections.emptyList())
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);

        assertThat(called).isTrue();
    }

    @Test
    public void testOnCompletionWithInvokeThrowingException() {
        AtomicBoolean called = new AtomicBoolean();
        Multi.createFrom().range(1, 5)
                .onCompletion().invoke(() -> {
                    called.set(true);
                    throw new RuntimeException("bam");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertFailedWith(RuntimeException.class, "bam");

        assertThat(called).isTrue();
    }

    @Test
    public void testOnCompletionContinueWithNullItem() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 5)
                .onCompletion().continueWith((Integer) null));
    }

    @Test
    public void testOnCompletionContinueWithNullAsIterable() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 5)
                .onCompletion().continueWith((Iterable<Integer>) null));
    }

    @Test
    public void testOnCompletionContinueWithItemsContainingNullItem() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 5)
                .onCompletion().continueWith(1, 2, 3, null, 4, 5));
    }

    @Test
    public void testOnCompletionContinueWithIterableContainingNullItem() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 5)
                .onCompletion().continueWith(Arrays.asList(1, 2, 3, null, 4, 5)));
    }

    @Test
    public void testOnCompletionContinueWithNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 5)
                .onCompletion().continueWith((Supplier<? extends Iterable<? extends Integer>>) null));
    }

    @Test
    public void testOnCompletionContinueWithSupplier() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith(() -> Arrays.asList(25, 26))
                .subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 25, 26);
    }

    @Test
    public void testOnCompletionContinueWithSupplierReturningEmpty() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith((Supplier<Iterable<? extends Integer>>) Collections::emptyList)
                .subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testOnCompletionContinueWithSupplierContainingNullItem() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith(() -> Arrays.asList(25, null, 26))
                .subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertFailedWith(NullPointerException.class, null)
                .assertItems(1, 2, 3, 4, 25);
    }

    @Test
    public void testOnCompletionContinueWithSupplierReturningNull() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith(() -> (Iterable<Integer>) null)
                .subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertFailedWith(NullPointerException.class, null)
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testOnCompletionContinueWithSupplierThrowingException() {
        Multi.createFrom().range(1, 5)
                .onCompletion().continueWith((Supplier<? extends Iterable<? extends Integer>>) () -> {
                    throw new IllegalStateException("BOOM!");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(20))
                .assertFailedWith(IllegalStateException.class, "BOOM!")
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testOnCompletionFail() {
        Multi.createFrom().range(1, 5)
                .onCompletion().fail()
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertItems(1, 2, 3, 4)
                .assertFailedWith(NoSuchElementException.class, null);
    }

    @Test
    public void testOnCompletionFailWithException() {
        Multi.createFrom().range(1, 5)
                .onCompletion().failWith(new IOException("boom"))
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertItems(1, 2, 3, 4)
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testOnCompletionFailWithNullException() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 5)
                .onCompletion().failWith((Throwable) null));

    }

    @Test
    public void testOnCompletionFailWithSupplier() {
        Multi.createFrom().range(1, 5)
                .onCompletion().failWith(() -> new IOException("boom"))
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertItems(1, 2, 3, 4)
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testOnCompletionFailWithNullSupplier() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().range(1, 5).onCompletion().failWith((Supplier<Throwable>) null));
    }

    @Test
    public void testOnCompletionFailWithSupplierThrowingException() {
        Multi.createFrom().range(1, 5)
                .onCompletion().failWith(() -> {
                    throw new IllegalStateException("BOOM!");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertItems(1, 2, 3, 4)
                .assertFailedWith(IllegalStateException.class, "BOOM!");
    }

    @Test
    public void testOnCompletionFailWithSupplierReturningNull() {
        Multi.createFrom().range(1, 5)
                .onCompletion().failWith(() -> null)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .assertItems(1, 2, 3, 4)
                .assertFailedWith(NullPointerException.class, null);
    }

    @Test
    public void testSwitchTo() {
        Multi.createFrom().range(1, 5)
                .onCompletion().switchTo(AdaptersToFlow.publisher(Flowable.just(20)))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 20);
    }

    @Test
    public void testSwitchToSupplier() {
        Multi.createFrom().range(1, 5)
                .onCompletion().switchTo(() -> Multi.createFrom().range(5, 8))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void testSwitchToSupplierReturningNull() {
        Multi.createFrom().range(1, 5)
                .onCompletion().switchTo(() -> null)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(NullPointerException.class, null)
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testSwitchToWithSupplierBeingNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 5)
                .onCompletion().switchTo((Supplier<Flow.Publisher<? extends Integer>>) null));
    }

    @Test
    public void testSwitchToWithConsumerBeingNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 5)
                .onCompletion().switchToEmitter(null));
    }

    @Test
    public void testSwitchToWithConsumer() {
        Multi.createFrom().range(1, 5)
                .onCompletion().switchToEmitter(e -> e.emit(5).emit(6).complete())
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 5, 6);
    }

    @Test
    public void testCall() {
        AtomicBoolean called = new AtomicBoolean();

        Multi.createFrom().range(1, 5)
                .onCompletion().call(() -> {
                    called.set(true);
                    return Uni.createFrom().item(69);
                })
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);

        assertThat(called).isTrue();
    }

    @Test
    public void testCallThatHasFailed() {
        AtomicBoolean called = new AtomicBoolean();

        Multi.createFrom().range(1, 5)
                .onCompletion().call(() -> {
                    called.set(true);
                    return Uni.createFrom().failure(new RuntimeException("bam"));
                })
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertItems(1, 2, 3, 4)
                .assertFailedWith(RuntimeException.class, "bam");

        assertThat(called).isTrue();
    }

    @Test
    public void testCallThatThrowsException() {
        AtomicBoolean called = new AtomicBoolean();

        Multi.createFrom().range(1, 5)
                .onCompletion().call(() -> {
                    called.set(true);
                    throw new RuntimeException("bam");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(7))
                .assertItems(1, 2, 3, 4)
                .assertFailedWith(RuntimeException.class, "bam");

        assertThat(called).isTrue();
    }

    @Test
    public void testCallCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicBoolean uniCancelled = new AtomicBoolean();
        AtomicInteger counter = new AtomicInteger();

        AssertSubscriber<Integer> subscriber = Multi.createFrom().range(1, 5)
                .onCompletion().call(() -> {
                    called.set(true);
                    counter.incrementAndGet();
                    return Uni.createFrom().emitter(e -> {
                        // do nothing
                    })
                            .onCancellation().invoke(() -> {
                                counter.incrementAndGet();
                                uniCancelled.set(true);
                            });
                })
                .subscribe().withSubscriber(AssertSubscriber.create(7));

        subscriber.assertItems(1, 2, 3, 4);
        subscriber.assertNotTerminated();
        assertThat(called.get()).isTrue();
        assertThat(uniCancelled.get()).isFalse();
        assertThat(counter.get()).isEqualTo(1);

        subscriber.cancel();
        subscriber.assertNotTerminated();
        assertThat(uniCancelled.get()).isTrue();
        assertThat(counter.get()).isEqualTo(2);

        subscriber.cancel();
        assertThat(counter.get()).isEqualTo(2);
    }

    @RepeatedTest(100)
    public void rogueEmittersInvoke() {
        AtomicInteger counter = new AtomicInteger();

        AssertSubscriber<Object> subscriber = Multi.createFrom()
                .emitter(e -> {
                    Thread t1 = new Thread(e::complete);
                    Thread t2 = new Thread(e::complete);
                    t1.start();
                    t2.start();
                    try {
                        t1.join();
                        t2.join();
                    } catch (InterruptedException interruptedException) {
                        throw new RuntimeException(interruptedException);
                    }
                })
                .onCompletion().invoke(counter::incrementAndGet)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertCompleted();
        assertThat(counter.get()).isEqualTo(1);
    }

    @RepeatedTest(100)
    public void rogueEmittersCall() {
        AtomicInteger counter = new AtomicInteger();

        AssertSubscriber<Object> subscriber = Multi.createFrom()
                .emitter(e -> {
                    Thread t1 = new Thread(e::complete);
                    Thread t2 = new Thread(e::complete);
                    t1.start();
                    t2.start();
                    try {
                        t1.join();
                        t2.join();
                    } catch (InterruptedException interruptedException) {
                        throw new RuntimeException(interruptedException);
                    }
                })
                .onCompletion().call(() -> {
                    counter.incrementAndGet();
                    return Uni.createFrom().item(69);
                })
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertCompleted();
        assertThat(counter.get()).isEqualTo(1);
    }
}
