package io.smallrye.reactive.unimulti.operators;

import io.smallrye.reactive.unimulti.Uni;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class UniOnItemIgnoreTest {

    @Test
    public void testIgnoreAndContinueWithNull() {
        Assertions.assertThat(Uni.createFrom().item(24)
                .onItem().ignore().andContinueWithNull().await().indefinitely()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIgnoreOnFailure() {
        Uni.createFrom().item(24).map(i -> {
            throw new IllegalArgumentException("BOOM");
        }).onItem().ignore().andContinueWithNull().await().indefinitely();
    }

    @Test
    public void testIgnoreAndFail() {
        UniAssertSubscriber<Integer> subscriber =
                Uni.createFrom().item(22).onItem().ignore().andFail().subscribe()
                        .withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailure(Exception.class, "");
    }

    @Test
    public void testIgnoreAndFailWith() {
        UniAssertSubscriber<Integer> subscriber =
                Uni.createFrom().item(22).onItem().ignore().andFail(new IOException("boom")).subscribe()
                        .withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailure(IOException.class, "boom");
    }

    @Test
    public void testIgnoreAndFailWithSupplier() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> boom = Uni.createFrom().item(22).onItem().ignore()
                .andFail(() -> new IOException("boom " + count.incrementAndGet()));
        UniAssertSubscriber<Integer> s1 = boom.subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Integer> s2 = boom.subscribe().withSubscriber(UniAssertSubscriber.create());
        s1.assertFailure(IOException.class, "boom 1");
        s2.assertFailure(IOException.class, "boom 2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIgnoreAndFailWithWithNullFailure() {
        Uni.createFrom().item(22).onItem().ignore().andFail((Exception) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIgnoreAndFailWithWithNullSupplier() {
        Uni.createFrom().item(22).onItem().ignore().andFail((Supplier<Throwable>) null);
    }

    @Test
    public void testIgnoreAndContinueWithValue() {
        Assertions.assertThat(Uni.createFrom().item(24).onItem().ignore().andContinueWith(42).await().indefinitely())
                .isEqualTo(42);
    }

    @Test
    public void testIgnoreAndContinueWithValueSupplier() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().item(24).onItem().ignore().andContinueWith(count::incrementAndGet);
        assertThat(uni.await().indefinitely()).isEqualTo(1);
        assertThat(uni.await().indefinitely()).isEqualTo(2);
    }

    @Test
    public void testIgnoreAndContinueWithValueSupplierReturningNull() {
        Assertions.assertThat(Uni.createFrom().item(24).onItem().ignore().andContinueWith(() -> null).await().indefinitely())
                .isEqualTo(null);
    }

    @Test
    public void testIgnoreAndSwitchToSupplier() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().item(24).onItem().ignore()
                .andSwitchTo(() -> Uni.createFrom().deferredItem(count::incrementAndGet));
        assertThat(uni.await().indefinitely()).isEqualTo(1);
        assertThat(uni.await().indefinitely()).isEqualTo(2);
    }

    @Test
    public void testIgnoreAndSwitchToUni() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().item(24).onItem().ignore()
                .andSwitchTo(Uni.createFrom().deferredItem(count::incrementAndGet));
        assertThat(uni.await().indefinitely()).isEqualTo(1);
        assertThat(uni.await().indefinitely()).isEqualTo(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIgnoreAndSwitchToNullSupplier() {
        Uni.createFrom().item(22).onItem().ignore().andSwitchTo((Supplier<Uni<?>>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIgnoreAndSwitchToNull() {
        Uni.createFrom().item(22).onItem().ignore().andSwitchTo((Uni<?>) null);
    }
}
