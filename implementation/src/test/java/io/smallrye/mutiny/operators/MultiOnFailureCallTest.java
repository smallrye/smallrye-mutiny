package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiOnFailureCallTest {

    public static final IOException BOOM = new IOException("boom");
    private final Multi<Integer> numbers = Multi.createFrom().items(1, 2);
    private final Multi<Integer> failed = Multi.createBy().concatenating()
            .streams(numbers, Multi.createFrom().failure(BOOM));
    private final Uni<Void> sub = Uni.createFrom().nullItem();

    @Test
    public void testCallOnItem() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicInteger twoGotCalled = new AtomicInteger();

        AssertSubscriber<Integer> subscriber = numbers.onFailure().call(i -> {
            failure.set(i);
            return sub.onItem().invoke(c -> twoGotCalled.incrementAndGet());
        }).subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertCompleted()
                .assertItems(1, 2);
        assertThat(twoGotCalled).hasValue(0);
        assertThat(failure).hasValue(null);
    }

    @Test
    public void testCallOnFailure() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicInteger twoGotCalled = new AtomicInteger();

        AssertSubscriber<Integer> subscriber = failed.onFailure().call(i -> {
            failure.set(i);
            return sub.onItem().invoke(c -> twoGotCalled.incrementAndGet());
        }).subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertFailedWith(IOException.class, "boom")
                .assertItems(1, 2);
        assertThat(twoGotCalled).hasValue(1);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testCallOnFailureWithSupplier() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicInteger twoGotCalled = new AtomicInteger();

        AssertSubscriber<Integer> subscriber = failed.onFailure().call(i -> {
            failure.set(i);
            return sub.onItem().invoke(twoGotCalled::incrementAndGet);
        }).subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertFailedWith(IOException.class, "boom")
                .assertItems(1, 2);
        assertThat(twoGotCalled).hasValue(1);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testFailureInAsyncCallback() {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        AssertSubscriber<Integer> subscriber = failed.onFailure().call(i -> {
            failure.set(i);
            throw new RuntimeException("kaboom");
        }).subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "kaboom")
                .assertItems(1, 2);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testNullReturnedByAsyncCallback() {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        AssertSubscriber<Integer> subscriber = failed.onFailure().call(i -> {
            failure.set(i);
            return null;
        }).subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "null")
                .assertItems(1, 2);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testCallWithSubFailure() {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        AssertSubscriber<Integer> subscriber = failed.onFailure().call(i -> {
            failure.set(i);
            return Uni.createFrom().failure(new IllegalStateException("d'oh"));
        }).subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "d'oh")
                .assertItems(1, 2);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testCancellationBeforeActionCompletes() {
        AtomicBoolean terminated = new AtomicBoolean();
        Uni<Object> uni = Uni.createFrom().emitter(e -> e.onTermination(() -> terminated.set(true)));

        AssertSubscriber<Integer> subscriber = failed.onFailure().call(i -> uni)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.cancel();
        assertThat(terminated).isTrue();
    }
}