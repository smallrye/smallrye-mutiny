package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.spies.MultiOnCancellationSpy;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiOnOverflowTest {

    @Test
    public void testThatDropCallbackCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().item(1).onOverflow().invoke((Consumer<Integer>) null).drop());
    }

    @Test
    public void testDropStrategy() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(20);
        Multi.createFrom().range(1, 10)
                .onOverflow().drop()
                .subscribe(sub);
        sub.assertCompleted()
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testDropStrategyOnHotStream() {
        AssertSubscriber<Long> sub = AssertSubscriber.create(20);
        Multi.createFrom().ticks().every(Duration.ofMillis(1))
                .onOverflow().drop()
                .emitOn(Infrastructure.getDefaultExecutor())
                .onItem().call(l -> Uni.createFrom().item(0).onItem().delayIt().by(Duration.ofMillis(2)))
                .subscribe(sub);

        await().until(() -> sub.getItems().size() == 20);
        sub.assertNotTerminated();
        sub.cancel();
    }

    @Test
    public void testDropStrategyWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .onOverflow().drop()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDropStrategyWithBackPressure() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(0);
        Multi.createFrom().range(1, 10)
                .onOverflow().drop()
                .subscribe(sub);
        sub.awaitCompletion().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testDropStrategyWithEmitter() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        List<Integer> list = new CopyOnWriteArrayList<>();
        Multi<Integer> multi = Multi.createFrom().emitter((Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onOverflow().invoke(list::add).drop();
        multi.subscribe(sub);
        emitter.get().emit(1);
        sub.request(2);
        emitter.get().emit(2).emit(3).emit(4);
        sub.request(1);
        emitter.get().emit(5).complete();
        sub
                .assertCompleted()
                .assertItems(2, 3, 5);
        assertThat(list).containsExactly(1, 4);
    }

    @Test
    public void testDropStrategyWithEmitterWithoutCallback() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(0);
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi<Integer> multi = Multi.createFrom().emitter((Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onOverflow().drop();
        multi.subscribe(sub);
        emitter.get().emit(1);
        sub.request(2);
        emitter.get().emit(2).emit(3).emit(4);
        sub.request(1);
        emitter.get().emit(5).complete();
        sub
                .assertCompleted()
                .assertItems(2, 3, 5);
    }

    @Test
    public void testDropStrategyWithCallbackThrowingAnException() {
        Multi.createFrom().items(2, 3, 4)
                .onOverflow().invoke(i -> {
                    throw new IllegalStateException("boom");
                }).drop()
                .subscribe().withSubscriber(AssertSubscriber.create(0))
                .assertFailedWith(IllegalStateException.class, "boom");

    }

    @Test
    public void testDropStrategyWithRequests() {
        Multi.createFrom().range(1, 10).onOverflow().drop()
                .subscribe().withSubscriber(AssertSubscriber.create(5))
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 5);
    }

    @Test
    public void testDropPreviousStrategy() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(20);
        Multi.createFrom().range(1, 10)
                .onOverflow().dropPreviousItems()
                .subscribe(sub);
        sub.assertCompleted()
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testDropPreviousStrategyWithBackPressure() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(1);
        Multi.createFrom().range(1, 1000)
                .onOverflow().dropPreviousItems()
                .subscribe(sub);
        sub.assertNotTerminated();

        sub.request(1000);
        sub.assertCompleted();
        assertThat(sub.getItems()).containsExactly(1, 999);

        sub = AssertSubscriber.create(0);
        Multi.createFrom().range(1, 1000)
                .onOverflow().dropPreviousItems()
                .subscribe(sub);
        sub.assertNotTerminated();

        sub.request(1000);
        sub.assertCompleted();
        assertThat(sub.getItems()).containsExactly(999);
    }

    @Test
    public void testDropPreviousStrategyWithEmitter() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi<Integer> multi = Multi.createFrom().emitter((Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onOverflow().dropPreviousItems();
        multi.subscribe(sub);

        emitter.get().emit(1);
        sub.assertNotTerminated().assertHasNotReceivedAnyItem();

        emitter.get().emit(2);
        sub.assertNotTerminated().assertHasNotReceivedAnyItem();

        sub.request(1);
        sub.assertNotTerminated().assertItems(2);

        emitter.get().emit(3).emit(4);

        sub.request(2);
        sub.assertNotTerminated().assertItems(2, 4);

        emitter.get().emit(5);
        sub.assertNotTerminated().assertItems(2, 4, 5);

        emitter.get().complete();
        sub.assertCompleted();
    }

    @Test
    public void testDropPreviousStrategyWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .onOverflow().dropPreviousItems()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testBufferStrategy() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(20);
        Multi.createFrom().range(1, 10)
                .onOverflow().buffer()
                .subscribe(sub);
        sub.assertCompleted()
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testBufferStrategyWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .onOverflow().buffer()
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testThatBufferSizeCannotBeNegative() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().item(1).onOverflow().buffer(-2));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().item(1).onOverflow().buffer(0));
    }

    @Test
    public void testBufferStrategyWithBackPressure() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(0);
        Multi.createFrom().range(1, 100)
                .onOverflow().buffer()
                .subscribe(sub);

        sub.request(5).assertItems(1, 2, 3, 4, 5);
        sub.request(90).assertNotTerminated();
        assertThat(sub.getItems()).hasSize(95).contains(94, 95, 20, 33);
        sub.request(5);
        assertThat(sub.getItems()).hasSize(99).endsWith(99);
    }

    @Test
    public void testBufferStrategyWithBufferTooSmall() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(5);
        Multi.createFrom().range(1, 100)
                .onOverflow().buffer(20)
                .subscribe(sub);

        sub.assertFailedWith(BackPressureFailure.class, null);
    }

    @Test
    public void testOnOverflowFailureShouldCancelTheUpstream() {
        AtomicReference<MultiSubscriber<? super Integer>> reference = new AtomicReference<>();
        AbstractMulti<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(mock(Flow.Subscription.class));
                reference.set(subscriber);
            }
        };
        MultiOnCancellationSpy<Integer> onCancellationSpy = Spy.onCancellation(rogue);

        AssertSubscriber<Integer> subscriber = onCancellationSpy.onOverflow().buffer(1)
                .subscribe().withSubscriber(AssertSubscriber.create(0));

        subscriber.assertSubscribed();
        assertThat(reference.get()).isNotNull();
        subscriber.assertNotTerminated();

        reference.get().onNext(1);
        reference.get().onNext(2);
        reference.get().onNext(3);
        reference.get().onNext(4);
        reference.get().onNext(5);

        subscriber.assertFailedWith(BackPressureFailure.class, "The overflow buffer is full");
        assertThat(onCancellationSpy.isCancelled()).isTrue();
    }

    @Test
    public void testDropInvokeConsumer() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        List<Integer> list = new CopyOnWriteArrayList<>();
        Multi<Integer> multi = Multi.createFrom().emitter((Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onOverflow().invoke(list::add).drop();
        multi.subscribe(sub);
        emitter.get().emit(1);
        sub.request(2);
        emitter.get().emit(2).emit(3).emit(4);
        sub.request(1);
        emitter.get().emit(5).complete();
        sub
                .assertCompleted()
                .assertItems(2, 3, 5);
        assertThat(list).containsExactly(1, 4);
    }

    @Test
    public void testDropInvokeCallback() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        List<Integer> list = new CopyOnWriteArrayList<>();
        Multi<Integer> multi = Multi.createFrom().emitter((Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onOverflow().invoke(() -> list.add(-1)).drop();
        multi.subscribe(sub);
        emitter.get().emit(1);
        sub.request(2);
        emitter.get().emit(2).emit(3).emit(4);
        sub.request(1);
        emitter.get().emit(5).complete();
        sub
                .assertCompleted()
                .assertItems(2, 3, 5);
        assertThat(list).containsExactly(-1, -1);
    }

    @Test
    public void testDropInvokeWithNullConsumer() {
        assertThatThrownBy(() -> Multi.createFrom().items(1, 2, 3)
                .onOverflow().invoke((Consumer<Integer>) null).drop(),
                "Null callbacks are forbidden", IllegalArgumentException.class);

    }

    @Test
    public void testDropInvokeWithNullCallback() {
        assertThatThrownBy(() -> Multi.createFrom().items(1, 2, 3)
                .onOverflow().invoke((Runnable) null).drop(),
                "Null callbacks are forbidden", IllegalArgumentException.class);

    }

    @Test
    public void testDropInvokeConsumerThrowingException() {
        Multi.createFrom().items(2, 3, 4)
                .onOverflow().invoke(i -> {
                    throw new IllegalStateException("boom");
                }).drop()
                .subscribe().withSubscriber(AssertSubscriber.create(0))
                .assertFailedWith(IllegalStateException.class, "boom");

    }

    @Test
    public void testDropCallWithNullMapper() {
        assertThatThrownBy(() -> Multi.createFrom().items(1, 2, 3)
                .onOverflow().call((Function<Integer, Uni<?>>) null).drop(),
                "Null callbacks are forbidden", IllegalArgumentException.class);
    }

    @Test
    public void testDropCallWithNullSupplier() {
        assertThatThrownBy(() -> Multi.createFrom().items(1, 2, 3)
                .onOverflow().call((Supplier<Uni<?>>) null).drop(),
                "Null callbacks are forbidden", IllegalArgumentException.class);
    }

    @Test
    public void testDropCallCallbackMapper() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        List<Integer> list = new CopyOnWriteArrayList<>();
        Multi<Integer> multi = Multi.createFrom().emitter((Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onOverflow().call(item -> {
                    list.add(item);
                    return Uni.createFrom().voidItem();
                }).drop();
        multi.subscribe(sub);
        emitter.get().emit(1);
        sub.request(2);
        emitter.get().emit(2).emit(3).emit(4);
        sub.request(1);
        emitter.get().emit(5).complete();
        sub
                .assertCompleted()
                .assertItems(2, 3, 5);
        assertThat(list).containsExactly(1, 4);
    }

    @Test
    public void testDropCallCallbackSupplier() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        List<Integer> list = new CopyOnWriteArrayList<>();
        Multi<Integer> multi = Multi.createFrom().emitter((Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onOverflow().call(() -> {
                    list.add(-1);
                    return Uni.createFrom().voidItem();
                }).drop();
        multi.subscribe(sub);
        emitter.get().emit(1);
        sub.request(2);
        emitter.get().emit(2).emit(3).emit(4);
        sub.request(1);
        emitter.get().emit(5).complete();
        sub
                .assertCompleted()
                .assertItems(2, 3, 5);
        assertThat(list).containsExactly(-1, -1);
    }

    @Test
    public void testDropCallFailedUni() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();

        Multi<Integer> multi = Multi.createFrom().emitter((Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onOverflow().call(item -> Uni.createFrom().failure(new IOException("boom :: " + item))).drop();
        multi.subscribe(sub);
        emitter.get().emit(1);

        sub.assertFailedWith(IOException.class, "boom :: 1");
    }

    @Test
    public void testDropCallThrowing() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();

        Multi<Integer> multi = Multi.createFrom().emitter((Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onOverflow().call(item -> {
                    throw new RuntimeException("boom :: " + item);
                }).drop();
        multi.subscribe(sub);
        emitter.get().emit(1);

        sub.assertFailedWith(RuntimeException.class, "boom :: 1");
    }

    @Test
    public void testBufferInvokeWithBackPressure() {
        AtomicInteger droppedItem = new AtomicInteger();

        AssertSubscriber<Integer> sub = AssertSubscriber.create(5);
        Multi.createFrom().range(1, 100)
                .onOverflow().invoke(droppedItem::set).buffer(20)
                .subscribe(sub);

        sub.assertFailedWith(BackPressureFailure.class, null);
        assertThat(droppedItem.get()).isEqualTo(26);
    }

    @Test
    public void testBufferInvokeThrowingWithBackPressure() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(5);
        Multi.createFrom().range(1, 100)
                .onOverflow().invoke(item -> {
                    throw new RuntimeException("boom :: " + item);
                }).buffer(20)
                .subscribe(sub);

        sub.assertFailedWith(BackPressureFailure.class, null);
        Throwable[] suppressed = sub.getFailure().getSuppressed();
        assertThat(suppressed).hasSize(1);
        assertThat(suppressed[0]).isInstanceOf(RuntimeException.class).hasMessage("boom :: 26");
    }

    @Test
    public void testBufferCallWithBackPressure() {
        AtomicInteger droppedItem = new AtomicInteger();

        AssertSubscriber<Integer> sub = AssertSubscriber.create(5);
        Multi.createFrom().range(1, 100)
                .onOverflow().call(item -> {
                    droppedItem.set(item);
                    return Uni.createFrom().voidItem();
                }).buffer(20)
                .subscribe(sub);

        sub.assertFailedWith(BackPressureFailure.class, null);
        assertThat(droppedItem.get()).isEqualTo(26);
    }

    @Test
    public void testBufferCallFailedWithBackPressure() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(5);
        Multi.createFrom().range(1, 100)
                .onOverflow().call(() -> Uni.createFrom().failure(new IOException("boom"))).buffer(20)
                .subscribe(sub);

        sub.assertFailedWith(BackPressureFailure.class, "");
        Throwable[] suppressed = sub.getFailure().getSuppressed();
        assertThat(suppressed).hasSize(1);
        assertThat(suppressed[0]).isInstanceOf(IOException.class).hasMessage("boom");
    }

    @Test
    public void testBufferCallThrowingWithBackPressure() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(5);
        Multi.createFrom().range(1, 100)
                .onOverflow().call(() -> {
                    throw new RuntimeException("boom");
                }).buffer(20)
                .subscribe(sub);

        sub.assertFailedWith(BackPressureFailure.class, "");
        Throwable[] suppressed = sub.getFailure().getSuppressed();
        assertThat(suppressed).hasSize(1);
        assertThat(suppressed[0]).isInstanceOf(RuntimeException.class).hasMessage("boom");
    }

    @Test
    public void testDropPreviousInvokeWithBackPressure() {
        List<Integer> list = new CopyOnWriteArrayList<>();

        AssertSubscriber<Integer> sub = AssertSubscriber.create(1);
        Multi.createFrom().range(1, 6)
                .onOverflow().invoke(list::add).dropPreviousItems()
                .subscribe(sub);

        sub.assertNotTerminated();
        assertThat(sub.getItems()).containsExactly(1);

        sub.request(10);
        assertThat(sub.getItems()).containsExactly(1, 5);
        sub.assertCompleted();

        assertThat(list).containsExactly(2, 3, 4, 5);
    }

    @Test
    public void testDropPreviousInvokeThrowingWithBackPressure() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(1);
        Multi.createFrom().range(1, 6)
                .onOverflow().invoke(item -> {
                    throw new RuntimeException("boom :: " + item);
                }).dropPreviousItems()
                .subscribe(sub);

        sub.assertFailedWith(RuntimeException.class, "boom :: 2");
    }

    @Test
    public void testDropPreviousCallWithBackPressure() {
        List<Integer> list = new CopyOnWriteArrayList<>();

        AssertSubscriber<Integer> sub = AssertSubscriber.create(1);
        Multi.createFrom().range(1, 6)
                .onOverflow().call(item -> {
                    list.add(item);
                    return Uni.createFrom().voidItem();
                }).dropPreviousItems()
                .subscribe(sub);

        sub.assertNotTerminated();
        assertThat(sub.getItems()).containsExactly(1);

        sub.request(10);
        assertThat(sub.getItems()).containsExactly(1, 5);
        sub.assertCompleted();

        assertThat(list).containsExactly(2, 3, 4, 5);
    }

    @Test
    public void testDropPreviousCallFailedWithBackPressure() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(1);
        Multi.createFrom().range(1, 6)
                .onOverflow().call(item -> Uni.createFrom().failure(new IOException("boom :: " + item))).dropPreviousItems()
                .subscribe(sub);

        sub.assertFailedWith(IOException.class, "boom :: 2");
    }

    @Test
    public void testDropPreviousCallThrowingWithBackPressure() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(1);
        Multi.createFrom().range(1, 6)
                .onOverflow().call(item -> {
                    throw new RuntimeException("boom :: " + item);
                }).dropPreviousItems()
                .subscribe(sub);

        sub.assertFailedWith(RuntimeException.class, "boom :: 2");
    }

    @Test
    public void boundedOverflowBufferShallSignalOverflow() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(1);
        Multi.createFrom().range(1, 1000)
                .onOverflow().buffer(20)
                .subscribe(sub);

        sub.assertFailedWith(BackPressureFailure.class);
    }

    @Test
    public void defaultBoundedOverflowBufferShallSignalOverflow() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(1);
        Multi.createFrom().range(1, 1000)
                .onOverflow().buffer()
                .subscribe(sub);

        sub.assertFailedWith(BackPressureFailure.class);
    }

    @Test
    public void unboundedOverflowShallNotSignalOverflow() {
        AssertSubscriber<Integer> sub = AssertSubscriber.create(1);
        Multi.createFrom().range(1, 1000)
                .onOverflow().bufferUnconditionally()
                .subscribe(sub);

        sub.assertNotTerminated();
        sub.request(Long.MAX_VALUE);
        sub.assertCompleted();
    }
}
