package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;

import io.smallrye.mutiny.Uni;

public class UniOnNullFailTest {

    @Test(expected = NoSuchElementException.class)
    public void testFail() {
        Uni.createFrom().item(null)
                .onItem().ifNull().fail().await().indefinitely();
    }

    @Test
    public void testFailNotCalledOnResult() {
        assertThat(Uni.createFrom().item(1).onItem().ifNull().fail().await().indefinitely()).isEqualTo(1);
    }

    @Test(expected = RuntimeException.class)
    public void testFailWithException() {
        Uni.createFrom().item(null).onItem().ifNull().failWith(new RuntimeException("boom")).await().indefinitely();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailWithExceptionSetToNull() {
        Uni.createFrom().item(null).onItem().ifNull().failWith((Exception) null).await().indefinitely();
    }

    @Test
    public void testFailWithExceptionNotCalledOnResult() {
        assertThat(Uni.createFrom().item(1).onItem().ifNull().failWith(new IOException("boom")).await().indefinitely())
                .isEqualTo(1);
    }

    @Test
    public void testFailWithExceptionSupplier() {
        AtomicInteger count = new AtomicInteger();
        Uni<Void> boom = Uni.createFrom().item(null)
                .onItem().castTo(Void.class)
                .onItem().ifNull().failWith(() -> new RuntimeException(Integer.toString(count.incrementAndGet())));

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> boom.await().indefinitely())
                .withMessageEndingWith("1");
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> boom.await().indefinitely())
                .withMessageEndingWith("2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailWithExceptionSupplierSetToNull() {
        Uni.createFrom().item(null).onItem().ifNull().failWith((Supplier<Throwable>) null).await().indefinitely();
    }

    @Test
    public void testFailWithExceptionSupplierNotCalledOnResult() {
        assertThat(Uni.createFrom().item(1).onItem().ifNull().failWith(new IOException("boom")).await().indefinitely())
                .isEqualTo(1);
    }

}
