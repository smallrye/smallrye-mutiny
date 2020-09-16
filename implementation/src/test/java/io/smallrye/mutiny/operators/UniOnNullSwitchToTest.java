package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

public class UniOnNullSwitchToTest {

    private final Uni<Integer> fallback = Uni.createFrom().item(23);
    private final Uni<Integer> failure = Uni.createFrom().failure(new IOException("boom"));

    @Test
    public void testSwitchToFallback() {
        assertThat(Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo(fallback)
                .await().indefinitely()).isEqualTo(23);
    }

    @Test
    public void testSwitchOnNonNull() {
        assertThat(Uni.createFrom().item(1).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo(fallback)
                .await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testSwitchToSupplierFallback() {
        AtomicInteger count = new AtomicInteger();
        assertThat(Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo(() -> fallback.map(i -> i + count.incrementAndGet()))
                .await().indefinitely()).isEqualTo(24);

        assertThat(Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo(() -> fallback.map(i -> i + count.incrementAndGet()))
                .await().indefinitely()).isEqualTo(25);
    }

    @Test
    public void testSwitchToFailure() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                        .onItem().ifNull().switchTo(failure)
                        .await().indefinitely())
                .withMessageEndingWith("boom");

    }

    @Test
    public void testSwitchToSupplierFailure() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                        .onItem().ifNull().switchTo(() -> failure)
                        .await().indefinitely())
                .withMessageEndingWith("boom");
    }

    @Test
    public void testSwitchToSupplierThrowingException() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                        .onItem().ifNull().switchTo(() -> {
                            throw new IllegalStateException("boom");
                        })
                        .await().indefinitely())
                .withMessageEndingWith("boom");

    }

    @Test
    public void testSwitchToNull() {
        assertThrows(NullPointerException.class, () -> Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo((Uni<Integer>) null)
                .await().indefinitely());
    }

    @Test
    public void testSwitchToNullSupplier() {
        assertThrows(NullPointerException.class, () -> Uni.createFrom().item((Object) null).onItem().castTo(Integer.class)
                .onItem().ifNull().switchTo((Uni<? extends Integer>) null)
                .await().indefinitely());
    }

}
