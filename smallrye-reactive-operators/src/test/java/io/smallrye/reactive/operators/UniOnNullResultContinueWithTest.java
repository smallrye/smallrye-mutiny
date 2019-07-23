package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class UniOnNullResultContinueWithTest {

    @Test
    public void testContinue() {
        assertThat(Uni.createFrom().nullValue()
                .onResult().castTo(Integer.class)
                .onNullResult().continueWith(42)
                .await().indefinitely()).isEqualTo(42);
    }

    @Test
    public void testContinueWithSupplier() {
        AtomicInteger counter = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().nullValue()
                .onResult().castTo(Integer.class)
                .onNullResult().continueWith(counter::incrementAndGet);
        assertThat(uni.await().indefinitely()).isEqualTo(1);
        assertThat(uni.await().indefinitely()).isEqualTo(2);
    }

    @Test
    public void testContinueNotCalledOnResult() {
        assertThat(Uni.createFrom().result(23)
                .onResult().castTo(Integer.class)
                .onNullResult().continueWith(42)
                .await().indefinitely()).isEqualTo(23);
    }

    @Test
    public void testContinueWithSupplierNotCalledOnResult() {
        assertThat(Uni.createFrom().result(23)
                .onResult().castTo(Integer.class)
                .onNullResult().continueWith(() -> 42)
                .await().indefinitely()).isEqualTo(23);
    }

    @Test
    public void testContinueNotCalledOnFailure() {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
                Uni.createFrom().failure(new IOException("boom"))
                        .onResult().castTo(Integer.class)
                        .onNullResult().continueWith(42)
                        .await().indefinitely()
        ).withCauseExactlyInstanceOf(IOException.class).withMessageEndingWith("boom");
    }

    @Test
    public void testContinueWithSupplierNotCalledOnFailure() {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
                Uni.createFrom().failure(new IOException("boom"))
                        .onResult().castTo(Integer.class)
                        .onNullResult().continueWith(() -> 42)
                        .await().indefinitely()
        ).withCauseExactlyInstanceOf(IOException.class).withMessageEndingWith("boom");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatContinueWithCannotUseNull() {
        Uni.createFrom().result(23)
                .onResult().castTo(Integer.class)
                .onNullResult().continueWith((Integer) null);
    }

    @Test(expected = NullPointerException.class)
    public void testThatContinueWithSupplierCannotReturnNull() {
        Uni.createFrom().result(23)
                .map(x -> null)
                .onNullResult().continueWith(() -> null)
                .await().indefinitely();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatContinueWithSupplierCannotBeNull() {
        Uni.createFrom().result(23)
                .onResult().castTo(Integer.class)
                .onNullResult().continueWith((Supplier<Integer>) null);
    }


}