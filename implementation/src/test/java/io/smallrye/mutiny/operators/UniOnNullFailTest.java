package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

public class UniOnNullFailTest {

    @Test
    public void testFail() {
        assertThrows(NoSuchElementException.class, () -> Uni.createFrom().item((Object) null)
                .onItem().ifNull().fail().await().indefinitely());
    }

    @Test
    public void testFailNotCalledOnItem() {
        assertThat(Uni.createFrom().item(1).onItem().ifNull().fail().await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testFailWithException() {
        assertThrows(RuntimeException.class,
                () -> Uni.createFrom().item((Object) null).onItem().ifNull().failWith(new RuntimeException("boom")).await()
                        .indefinitely());
    }

    @Test
    public void testFailWithExceptionSetToNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item((Object) null).onItem().ifNull().failWith((Exception) null).await().indefinitely());
    }

    @Test
    public void testFailWithExceptionNotCalledOnITem() {
        assertThat(Uni.createFrom().item(1).onItem().ifNull().failWith(new IOException("boom")).await().indefinitely())
                .isEqualTo(1);
    }

    @Test
    public void testFailWithExceptionSupplier() {
        AtomicInteger count = new AtomicInteger();
        Uni<Void> boom = Uni.createFrom().item((Object) null)
                .onItem().castTo(Void.class)
                .onItem().ifNull().failWith(() -> new RuntimeException(Integer.toString(count.incrementAndGet())));

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> boom.await().indefinitely())
                .withMessageEndingWith("1");
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> boom.await().indefinitely())
                .withMessageEndingWith("2");
    }

    @Test
    public void testFailWithExceptionSupplierSetToNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item((Object) null).onItem().ifNull().failWith((Supplier<Throwable>) null).await()
                        .indefinitely());
    }

    @Test
    public void estFailWithExceptionSupplierThrowingException() {
        assertThrows(IllegalStateException.class,
                () -> Uni.createFrom().item((Object) null).onItem().ifNull().failWith(() -> {
                    throw new IllegalStateException("boom");
                }).await().indefinitely());
    }

    @Test
    public void testFailWithExceptionSupplierReturningNull() {
        assertThrows(NullPointerException.class,
                () -> Uni.createFrom().item((Object) null).onItem().ifNull().failWith(() -> null).await().indefinitely());
    }

    @Test
    public void testFailWithExceptionSupplierNotCalledOnItem() {
        assertThat(Uni.createFrom().item(1).onItem().ifNull().failWith(new IOException("boom")).await().indefinitely())
                .isEqualTo(1);
    }

}
