package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class UniOnNullResultSwitchToTest {

    private Uni<Integer> fallback = Uni.createFrom().result(23);
    private Uni<Integer> failure = Uni.createFrom().failure(new IOException("boom"));

    @Test
    public void testSwitchToFallback() {
        assertThat(Uni.createFrom().nullValue().onResult().castTo(Integer.class)
                .onNullResult().switchTo(fallback)
                .await().indefinitely()).isEqualTo(23);
    }

    @Test
    public void testSwitchToSupplierFallback() {
        AtomicInteger count = new AtomicInteger();
        assertThat(Uni.createFrom().nullValue().onResult().castTo(Integer.class)
                .onNullResult().switchTo(() -> fallback.map(i -> i + count.incrementAndGet()))
                .await().indefinitely()).isEqualTo(24);

        assertThat(Uni.createFrom().nullValue().onResult().castTo(Integer.class)
                .onNullResult().switchTo(() -> fallback.map(i -> i + count.incrementAndGet()))
                .await().indefinitely()).isEqualTo(25);
    }

    @Test
    public void testSwitchToFailure() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() ->
                        Uni.createFrom().nullValue().onResult().castTo(Integer.class)
                                .onNullResult().switchTo(failure)
                                .await().indefinitely()
                ).withMessageEndingWith("boom");

    }

    @Test
    public void testSwitchToSupplierFailure() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() ->
                        Uni.createFrom().nullValue().onResult().castTo(Integer.class)
                                .onNullResult().switchTo(() -> failure)
                                .await().indefinitely()
                ).withMessageEndingWith("boom");

    }

    @Test(expected = NullPointerException.class)
    public void testSwitchToNull() {
        Uni.createFrom().nullValue().onResult().castTo(Integer.class)
                .onNullResult().switchTo((Uni<Integer>) null)
                .await().indefinitely();
    }

    @Test(expected = NullPointerException.class)
    public void testSwitchToNullSupplier() {
        Uni.createFrom().nullValue().onResult().castTo(Integer.class)
                .onNullResult().switchTo((Uni<? extends Integer>) null)
                .await().indefinitely();
    }

}