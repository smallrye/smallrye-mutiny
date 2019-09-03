package io.smallrye.reactive.operators;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.subscription.BackPressureFailure;
import io.smallrye.reactive.subscription.MultiEmitter;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class MultiOnOverflowTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThatDropCallbackCannotBeNull() {
        Multi.createFrom().item(1).onOverflow().drop(null);
    }

    @Test
    public void testDropStrategy() {
        MultiAssertSubscriber<Integer> sub = MultiAssertSubscriber.create(20);
        Multi.createFrom().range(1, 10)
                .onOverflow().drop()
                .subscribe(sub);
        sub.assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testDropStrategyWithUpstreamFailure() {
        Multi.createFrom().<Integer>failure(new IOException("boom"))
                .onOverflow().drop()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testDropStrategyWithBackPressure() {
        MultiAssertSubscriber<Integer> sub = MultiAssertSubscriber.create();
        Multi.createFrom().range(1, 10)
                .onOverflow().drop()
                .subscribe(sub);

        sub.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testDropStrategyWithEmitter() {
        MultiAssertSubscriber<Integer> sub = MultiAssertSubscriber.create();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        List<Integer> list = new CopyOnWriteArrayList<>();
        Multi<Integer> multi = Multi.createFrom().emitter((Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onOverflow().drop(list::add);
        multi.subscribe(sub);
        emitter.get().emit(1);
        sub.request(2);
        emitter.get().emit(2).emit(3).emit(4);
        sub.request(1);
        emitter.get().emit(5).complete();
        sub
                .assertCompletedSuccessfully()
                .assertReceived(2, 3, 5);
        assertThat(list).containsExactly(1, 4);
    }

    @Test
    public void testDropStrategyWithEmitterWithoutCallback() {
        MultiAssertSubscriber<Integer> sub = MultiAssertSubscriber.create();
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
                .assertCompletedSuccessfully()
                .assertReceived(2, 3, 5);
    }

    @Test
    public void testDropStrategyWithCallbackThrowingAnException() {
        Multi.createFrom().items(2, 3, 4)
                .onOverflow().drop(i -> {
            throw new IllegalStateException("boom");
        })
                .subscribe().withSubscriber(MultiAssertSubscriber.create())
                .assertHasFailedWith(IllegalStateException.class, "boom");

    }

    @Test
    public void testDropStrategyWithRequests() {
        Multi.createFrom().range(1, 10).onOverflow().drop()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(5))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5);
    }

    @Test
    public void testDropPreviousStrategy() {
        MultiAssertSubscriber<Integer> sub = MultiAssertSubscriber.create(20);
        Multi.createFrom().range(1, 10)
                .onOverflow().dropPreviousItems()
                .subscribe(sub);
        sub.assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testDropPreviousStrategyWithBackPressure() {
        MultiAssertSubscriber<Integer> sub = MultiAssertSubscriber.create(1);
        Multi.createFrom().range(1, 1000)
                .onOverflow().dropPreviousItems()
                .subscribe(sub);
        sub.assertNotTerminated();

        sub.request(1000);
        sub.assertCompletedSuccessfully();
        assertThat(sub.items()).containsExactly(1, 999);

        sub = MultiAssertSubscriber.create(0);
        Multi.createFrom().range(1, 1000)
                .onOverflow().dropPreviousItems()
                .subscribe(sub);
        sub.assertNotTerminated();

        sub.request(1000);
        sub.assertCompletedSuccessfully();
        assertThat(sub.items()).containsExactly(999);
    }

    @Test
    public void testDropPreviousStrategyWithEmitter() {
        MultiAssertSubscriber<Integer> sub = MultiAssertSubscriber.create();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi<Integer> multi = Multi.createFrom().emitter((Consumer<MultiEmitter<? super Integer>>) emitter::set)
                .onOverflow().dropPreviousItems();
        multi.subscribe(sub);

        emitter.get().emit(1);
        sub.assertNotTerminated().assertHasNotReceivedAnyItem();

        emitter.get().emit(2);
        sub.assertNotTerminated().assertHasNotReceivedAnyItem();

        sub.request(1);
        sub.assertNotTerminated().assertReceived(2);

        emitter.get().emit(3).emit(4);

        sub.request(2);
        sub.assertNotTerminated().assertReceived(2, 4);

        emitter.get().emit(5);
        sub.assertNotTerminated().assertReceived(2, 4, 5);

        emitter.get().complete();
        sub.assertCompletedSuccessfully();
    }

    @Test
    public void testDropPreviousStrategyWithUpstreamFailure() {
        Multi.createFrom().<Integer>failure(new IOException("boom"))
                .onOverflow().dropPreviousItems()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testBufferStrategy() {
        MultiAssertSubscriber<Integer> sub = MultiAssertSubscriber.create(20);
        Multi.createFrom().range(1, 10)
                .onOverflow().buffer()
                .subscribe(sub);
        sub.assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testBufferStrategyWithUpstreamFailure() {
        Multi.createFrom().<Integer>failure(new IOException("boom"))
                .onOverflow().buffer()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(IOException.class, "boom");
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
        MultiAssertSubscriber<Integer> sub = MultiAssertSubscriber.create(0);
        Multi.createFrom().range(1, 100)
                .onOverflow().buffer()
                .subscribe(sub);

        sub.request(5).assertReceived(1, 2, 3, 4, 5);
        sub.request(90).assertNotTerminated();
        assertThat(sub.items()).hasSize(95).contains(94, 95, 20, 33);
        sub.request(5);
        assertThat(sub.items()).hasSize(99).endsWith(99);
    }

    @Test
    public void testBufferStrategyWithBufferTooSmall() {
        MultiAssertSubscriber<Integer> sub = MultiAssertSubscriber.create(5);
        Multi.createFrom().range(1, 100)
                .onOverflow().buffer(20)
                .subscribe(sub);

        sub.assertHasFailedWith(BackPressureFailure.class, null);
    }



}
