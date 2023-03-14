package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class UniOnItemFailWithTest {

    private final Uni<Integer> one = Uni.createFrom().item(1);

    @Test
    public void testWithNullSupplierOrFunction() {
        assertThatThrownBy(() -> one.onItem().failWith((Function<? super Integer, ? extends Throwable>) null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mapper");

        assertThatThrownBy(() -> Uni.createFrom().item(1).onItem().failWith((Supplier<? extends Throwable>) null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("supplier");
    }

    @Test
    public void testSupplierNotCalledOnUpstreamFailure() {
        AtomicBoolean called = new AtomicBoolean();
        one.onItem().failWith((Supplier<Throwable>) TestException::new)
                .onItem().failWith(() -> {
                    called.set(true);
                    return new IOException();
                })
                .subscribe().withSubscriber(UniAssertSubscriber.<Number> create())
                .assertFailedWith(TestException.class, "");

        assertThat(called).isFalse();
    }

    @Test
    public void testFunctionNotCalledOnUpstreamFailure() {
        AtomicBoolean called = new AtomicBoolean();
        one.onItem().failWith((Supplier<Throwable>) TestException::new)
                .onItem().failWith(item -> {
                    called.set(true);
                    return new IOException(Integer.toString(item));
                })
                .subscribe().withSubscriber(UniAssertSubscriber.<Number> create())
                .assertFailedWith(TestException.class, "");

        assertThat(called).isFalse();
    }

    @Test
    public void testSupplierNotCalledOnImmediateCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> subscriber = one
                .onItem().failWith(() -> {
                    called.set(true);
                    return new IOException();
                })
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        subscriber.assertSubscribed().assertNotTerminated();
        assertThat(called).isFalse();
    }

    @Test
    public void testFunctionNotCalledOnImmediateCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> subscriber = one
                .onItem().failWith(item -> {
                    called.set(true);
                    return new IOException();
                })
                .subscribe().withSubscriber(new UniAssertSubscriber<>(true));

        subscriber.assertSubscribed().assertNotTerminated();
        assertThat(called).isFalse();
    }

    @Test
    public void testFailWithFunction() {
        assertThatThrownBy(() -> one
                .onItem().failWith(i -> new TestException("boom-" + i))
                .await().indefinitely()).isInstanceOf(TestException.class).hasMessage("boom-1");

        assertThatThrownBy(() -> one
                .onItem().failWith(i -> new IOException("boom-" + i))
                .await().indefinitely()).isInstanceOf(CompletionException.class).hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("boom-1");
    }

    @Test
    public void testFailWithFunctionWithSubscriber() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = one.onItem().failWith(s -> new IOException(Integer.toString(s + count.getAndIncrement())));
        uni
                .subscribe().withSubscriber(UniAssertSubscriber.<Number> create())
                .assertFailedWith(IOException.class, "1");
        uni
                .subscribe().withSubscriber(UniAssertSubscriber.<Number> create())
                .assertFailedWith(IOException.class, "2");
    }

    @Test
    public void testFailWithSupplier() {
        assertThatThrownBy(() -> one
                .onItem().failWith(() -> new TestException("boom"))
                .await().indefinitely()).isInstanceOf(TestException.class).hasMessage("boom");

        assertThatThrownBy(() -> one
                .onItem().failWith(() -> new IOException("boom"))
                .await().indefinitely()).isInstanceOf(CompletionException.class).hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("boom");
    }

    @Test
    public void testFailWithSupplierWithSubscriber() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = one.onItem().failWith(() -> new IOException(Integer.toString(count.getAndIncrement())));
        uni
                .subscribe().withSubscriber(UniAssertSubscriber.<Number> create())
                .assertFailedWith(IOException.class, "0");
        uni
                .subscribe().withSubscriber(UniAssertSubscriber.<Number> create())
                .assertFailedWith(IOException.class, "1");
    }

    @Test
    public void testSupplierOrFunctionReturningNull() {
        assertThatThrownBy(() -> one.onItem().failWith(item -> null).await().indefinitely())
                .isInstanceOf(NullPointerException.class).hasMessageContaining("mapper");

        assertThatThrownBy(() -> one.onItem().failWith(() -> null).await().indefinitely())
                .isInstanceOf(NullPointerException.class).hasMessageContaining("supplier");
    }

    @Test
    public void testSupplierOrFunctionThrowingException() {
        assertThatThrownBy(() -> one.onItem().failWith(item -> {
            throw new IllegalArgumentException("boom-" + item);
        }).await().indefinitely()).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("boom-1");

        assertThatThrownBy(() -> one.onItem().failWith(() -> {
            throw new IllegalArgumentException("boom");
        }).await().indefinitely()).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("boom");
    }

}
