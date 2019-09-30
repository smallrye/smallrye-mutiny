package io.smallrye.reactive.unimulti.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.smallrye.reactive.unimulti.Uni;

public class UniOnNullSwitchToTest {

    private Uni<Integer> fallback = Uni.createFrom().item(23);
    private Uni<Integer> failure = Uni.createFrom().failure(new IOException("boom"));

    @Test
    public void testSwitchToFallback() {
        assertThat(Uni.createFrom().item(null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo(fallback)
                .await().indefinitely()).isEqualTo(23);
    }

    @Test
    public void testSwitchToSupplierFallback() {
        AtomicInteger count = new AtomicInteger();
        assertThat(Uni.createFrom().item(null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo(() -> fallback.map(i -> i + count.incrementAndGet()))
                .await().indefinitely()).isEqualTo(24);

        assertThat(Uni.createFrom().item(null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo(() -> fallback.map(i -> i + count.incrementAndGet()))
                .await().indefinitely()).isEqualTo(25);
    }

    @Test
    public void testSwitchToFailure() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> Uni.createFrom().item(null).onItem().castTo(Integer.class)
                        .onItem().ifNull().switchTo(failure)
                        .await().indefinitely())
                .withMessageEndingWith("boom");

    }

    @Test
    public void testSwitchToSupplierFailure() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> Uni.createFrom().item(null).onItem().castTo(Integer.class)
                        .onItem().ifNull().switchTo(() -> failure)
                        .await().indefinitely())
                .withMessageEndingWith("boom");

    }

    @Test(expected = NullPointerException.class)
    public void testSwitchToNull() {
        Uni.createFrom().item(null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo((Uni<Integer>) null)
                .await().indefinitely();
    }

    @Test(expected = NullPointerException.class)
    public void testSwitchToNullSupplier() {
        Uni.createFrom().item(null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo((Uni<? extends Integer>) null)
                .await().indefinitely();
    }

}
