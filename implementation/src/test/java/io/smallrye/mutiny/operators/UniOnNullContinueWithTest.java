package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UniOnNullContinueWithTest {

    @Test
    public void testContinue() {
        assertThat(Uni.createFrom().item((Object) null)
                .onItem().castTo(Integer.class)
                .onItem().ifNull().continueWith(42)
                .await().indefinitely()).isEqualTo(42);
    }

    @Test
    public void testContinueWithSupplier() {
        AtomicInteger counter = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().item((Object) null)
                .onItem().castTo(Integer.class)
                .onItem().ifNull().continueWith(counter::incrementAndGet);
        assertThat(uni.await().indefinitely()).isEqualTo(1);
        assertThat(uni.await().indefinitely()).isEqualTo(2);
    }

    @Test
    public void testContinueNotCalledOnResult() {
        assertThat(Uni.createFrom().item(23)
                .onItem().castTo(Integer.class)
                .onItem().ifNull().continueWith(42)
                .await().indefinitely()).isEqualTo(23);
    }

    @Test
    public void testContinueWithSupplierNotCalledOnResult() {
        assertThat(Uni.createFrom().item(23)
                .onItem().castTo(Integer.class)
                .onItem().ifNull().continueWith(() -> 42)
                .await().indefinitely()).isEqualTo(23);
    }

    @Test
    public void testContinueNotCalledOnFailure() {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> Uni.createFrom().failure(new IOException("boom"))
                .onItem().castTo(Integer.class)
                .onItem().ifNull().continueWith(42)
                .await().indefinitely()).withCauseExactlyInstanceOf(IOException.class).withMessageEndingWith("boom");
    }

    @Test
    public void testContinueWithSupplierNotCalledOnFailure() {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> Uni.createFrom().failure(new IOException("boom"))
                .onItem().castTo(Integer.class)
                .onItem().ifNull().continueWith(() -> 42)
                .await().indefinitely()).withCauseExactlyInstanceOf(IOException.class).withMessageEndingWith("boom");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatContinueWithCannotUseNull() {
        Uni.createFrom().item(23)
                .onItem().castTo(Integer.class)
                .onItem().ifNull().continueWith((Integer) null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testThatContinueWithSupplierCannotReturnNull() {
        Uni.createFrom().item(23)
                .map(x -> null)
                .onItem().ifNull().continueWith(() -> null)
                .await().indefinitely();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatContinueWithSupplierCannotBeNull() {
        Uni.createFrom().item(23)
                .onItem().castTo(Integer.class)
                .onItem().ifNull().continueWith((Supplier<Integer>) null);
    }

}
