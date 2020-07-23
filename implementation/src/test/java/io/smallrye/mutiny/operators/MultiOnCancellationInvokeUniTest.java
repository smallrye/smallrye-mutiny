package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiOnCancellationInvokeUniTest {

    @Test
    public void testCancellationWithNoRequestedItem() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onItem().invoke(item::set)
                .onCancellation().invokeUni(() -> {
                    cancellation.set(true);
                    return Uni.createFrom().item("value");
                })
                .subscribe().withSubscriber(ts);

        ts.cancel()
                .assertHasNotReceivedAnyItem()
                .assertHasNotCompleted();

        assertThat(item.get()).isNull();
        assertThat(cancellation.get()).isTrue();
    }

    @Test
    public void testCancellationWithNoRequestedItemAndFailedUni() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onItem().invoke(item::set)
                .onCancellation().invokeUni(() -> {
                    cancellation.set(true);
                    return Uni.createFrom().failure(new RuntimeException("bam"));
                })
                .subscribe().withSubscriber(ts);

        ts.cancel()
                .assertHasNotReceivedAnyItem()
                .assertHasNotCompleted();

        assertThat(item.get()).isNull();
        assertThat(cancellation.get()).isTrue();
    }

    @Test
    public void testCancellationWithNoRequestedItemAndThrowingInvokeUni() {
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        AtomicReference<Integer> item = new AtomicReference<>();
        AtomicBoolean cancellation = new AtomicBoolean();

        Multi.createFrom().item(1)
                .onItem().invoke(item::set)
                .onCancellation().invokeUni(() -> {
                    cancellation.set(true);
                    throw new RuntimeException("bam");
                })
                .subscribe().withSubscriber(ts);

        ts.cancel()
                .assertHasNotReceivedAnyItem()
                .assertHasNotCompleted();

        assertThat(item.get()).isNull();
        assertThat(cancellation.get()).isTrue();
    }

    @Test
    public void testCancellationAfterOneItem() {
        MultiAssertSubscriber<Object> ts = MultiAssertSubscriber.create();

        AtomicReference<Object> item = new AtomicReference<>();
        AtomicBoolean cancellation = new AtomicBoolean();
        AtomicInteger counter = new AtomicInteger();

        Multi.createFrom().emitter(e -> {
            e.emit(1);
        })
                .onItem().invoke(item::set)
                .onCancellation().invokeUni(() -> {
                    counter.incrementAndGet();
                    cancellation.set(true);
                    return Uni.createFrom().item("value");
                })
                .subscribe().withSubscriber(ts);

        ts.request(10);

        assertThat(item.get()).isEqualTo(1);
        assertThat(cancellation.get()).isFalse();
        assertThat(counter.get()).isEqualTo(0);

        ts.cancel()
                .assertHasNotCompleted()
                .assertHasNotFailed();

        assertThat(cancellation.get()).isTrue();
        assertThat(counter.get()).isEqualTo(1);

        ts.cancel();
        assertThat(counter.get()).isEqualTo(1);
    }
}
