package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiOnCancellationCallTest {

    @Test
    public void testCancellationWithNoRequestedItem() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onItem().invoke(item::set)
                .onCancellation().call(() -> {
                    cancellation.set(true);
                    return Uni.createFrom().item("value");
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.cancel()
                .assertHasNotReceivedAnyItem()
                .assertNotTerminated();

        assertThat(item.get()).isNull();
        assertThat(cancellation.get()).isTrue();
    }

    @Test
    public void testCancellationWithNoRequestedItemAndFailedUni() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onItem().invoke(item::set)
                .onCancellation().call(() -> {
                    cancellation.set(true);
                    return Uni.createFrom().failure(new RuntimeException("bam"));
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.cancel()
                .assertHasNotReceivedAnyItem()
                .assertNotTerminated();

        assertThat(item.get()).isNull();
        assertThat(cancellation.get()).isTrue();
    }

    @Test
    public void testCancellationWithNoRequestedItemAndThrowingCall() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onItem().invoke(item::set)
                .onCancellation().call(() -> {
                    cancellation.set(true);
                    throw new RuntimeException("bam");
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.cancel()
                .assertHasNotReceivedAnyItem()
                .assertNotTerminated();

        assertThat(item.get()).isNull();
        assertThat(cancellation.get()).isTrue();
    }

    @Test
    public void testCancellationAfterOneItem() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();

        AtomicReference<Object> item = new AtomicReference<>();
        AtomicBoolean cancellation = new AtomicBoolean();
        AtomicInteger counter = new AtomicInteger();

        Multi.createFrom().emitter(e -> {
            e.emit(1);
        })
                .onItem().invoke(item::set)
                .onCancellation().call(() -> {
                    counter.incrementAndGet();
                    cancellation.set(true);
                    return Uni.createFrom().item("value");
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.request(10);

        assertThat(item.get()).isEqualTo(1);
        assertThat(cancellation.get()).isFalse();
        assertThat(counter.get()).isEqualTo(0);

        subscriber.cancel().assertNotTerminated();

        assertThat(cancellation.get()).isTrue();
        assertThat(counter.get()).isEqualTo(1);

        subscriber.cancel();
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    public void testCancellationPropagatedBeforeCompletion() {
        AtomicBoolean invokeCalled = new AtomicBoolean();
        AtomicBoolean callCalled = new AtomicBoolean();

        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onCancellation().invoke(() -> invokeCalled.set(true))
                .onCancellation().invoke(() -> callCalled.set(true));

        AssertSubscriber<Integer> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber.cancel();
        subscriber.assertNotTerminated();
        assertThat(invokeCalled.get()).isTrue();
        assertThat(callCalled.get()).isTrue();
    }

    @Test
    public void testCancellationNotPropagatedAfterCompletion() {
        AtomicBoolean invokeCalled = new AtomicBoolean();
        AtomicBoolean callCalled = new AtomicBoolean();

        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3)
                .onCancellation().invoke(() -> invokeCalled.set(true))
                .onCancellation().invoke(() -> callCalled.set(true));

        AssertSubscriber<Integer> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(69L));
        subscriber.assertCompleted().assertItems(1, 2, 3);
        subscriber.cancel();
        assertThat(invokeCalled.get()).isFalse();
        assertThat(callCalled.get()).isFalse();
    }
}
