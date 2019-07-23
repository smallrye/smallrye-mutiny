package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class UniOnNullResultFailTest {


    @Test(expected = NoSuchElementException.class)
    public void testFail() {
        Uni.createFrom().nullValue()
                .onNullResult().fail().await().indefinitely();
    }

    @Test
    public void testFailNotCalledOnResult() {
        assertThat(Uni.createFrom().result(1).onNullResult().fail().await().indefinitely()).isEqualTo(1);
    }

    @Test(expected = RuntimeException.class)
    public void testFailWithException() {
        Uni.createFrom().nullValue().onNullResult().failWith(new RuntimeException("boom")).await().indefinitely();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailWithExceptionSetToNull() {
        Uni.createFrom().nullValue().onNullResult().failWith((Exception) null).await().indefinitely();
    }

    @Test
    public void testFailWithExceptionNotCalledOnResult() {
        assertThat(Uni.createFrom().result(1).onNullResult().failWith(new IOException("boom")).await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testFailWithExceptionSupplier() {
        AtomicInteger count = new AtomicInteger();
        Uni<Void> boom = Uni.createFrom().nullValue().onNullResult().failWith(() -> new RuntimeException(Integer.toString(count.incrementAndGet())));

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> boom.await().indefinitely()).withMessageEndingWith("1");
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> boom.await().indefinitely()).withMessageEndingWith("2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailWithExceptionSupplierSetToNull() {
        Uni.createFrom().nullValue().onNullResult().failWith((Supplier<Throwable>) null).await().indefinitely();
    }

    @Test
    public void testFailWithExceptionSupplierNotCalledOnResult() {
        assertThat(Uni.createFrom().result(1).onNullResult().failWith(new IOException("boom")).await().indefinitely()).isEqualTo(1);
    }


}