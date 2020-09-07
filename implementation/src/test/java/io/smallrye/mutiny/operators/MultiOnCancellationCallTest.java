package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.test.AssertSubscriber;

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
                .assertHasNotCompleted();

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
                .assertHasNotCompleted();

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
                .assertHasNotCompleted();

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

        subscriber.cancel()
                .assertHasNotCompleted()
                .assertHasNotFailed();

        assertThat(cancellation.get()).isTrue();
        assertThat(counter.get()).isEqualTo(1);

        subscriber.cancel();
        assertThat(counter.get()).isEqualTo(1);
    }
}
