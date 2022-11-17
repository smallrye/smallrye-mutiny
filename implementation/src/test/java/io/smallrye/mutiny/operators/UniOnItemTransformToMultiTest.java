package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class UniOnItemTransformToMultiTest {

    @Test
    public void testTransformToMultiWithNullMapper() {
        assertThrows(IllegalArgumentException.class, () -> {
            Uni<Integer> uni = Uni.createFrom().item(1);
            uni.onItem().transformToMulti(null);
        });
    }

    @Test
    public void testTransformToMultiWithItem() {
        Uni.createFrom().item(1)
                .onItem().transformToMulti(i -> Multi.createFrom().range(i, 5))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitCompletion()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testTransformToMultiWithNull() {
        Uni.createFrom().voidItem()
                .onItem().transformToMulti(x -> Multi.createFrom().range(1, 5))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitCompletion()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testTransformToMultiWithFailure() {
        Uni.createFrom().<Integer> failure(new IOException("boom"))
                .onItem().transformToMulti(x -> Multi.createFrom().range(1, 5))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitFailure()
                .assertFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTransformToMultiWithExceptionThrownByMapper() {
        Uni.createFrom().item(1)
                .onItem().transformToMulti(x -> {
                    throw new IllegalStateException("boom");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitFailure()
                .assertFailedWith(IllegalStateException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTransformToMultiWithNullReturnedByMapper() {
        Uni.createFrom().item(1)
                .onItem().transformToMulti(x -> null)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitFailure()
                .assertFailedWith(NullPointerException.class, "")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTransformToMultiWithNullReturnedByMapperWithCancellationDuringTheUniResolution() {
        final AtomicBoolean called = new AtomicBoolean();

        Uni.createFrom().<Integer> nothing()
                .onCancellation().invoke(() -> called.set(true))
                .onItem().transformToMulti(x -> Multi.createFrom().range(x, 10))
                .subscribe().withSubscriber(AssertSubscriber.create(10))

                .assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .run(() -> assertThat(called).isFalse())
                .cancel()
                .run(() -> assertThat(called).isTrue())
                .assertNotTerminated();
    }

    @Test
    public void testTransformToMultiWithNullReturnedByMapperWithCancellationDuringTheMultiEmissions() {
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicBoolean calledUni = new AtomicBoolean();

        Uni.createFrom().item(1)
                .onCancellation().invoke(() -> calledUni.set(true))
                .onItem().transformToMulti(i -> Multi.createFrom().nothing()
                        .onCancellation().invoke(() -> called.set(true)))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .run(() -> assertThat(called).isFalse())
                .cancel()
                .run(() -> assertThat(called).isTrue())
                .run(() -> assertThat(calledUni).isFalse())
                .assertNotTerminated();
    }
}
