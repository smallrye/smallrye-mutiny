package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.spies.MultiOnCancellationSpy;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiOnItemFailWithTest {

    private MultiOnCancellationSpy<Integer> items;
    private MultiOnCancellationSpy<Integer> nothing;

    @BeforeEach
    public void prepare() {
        items = Spy.onCancellation(Multi.createFrom().items(1, 2));
        nothing = Spy.onCancellation(Multi.createFrom().nothing());
    }

    @Test
    public void testWithNullSupplierOrFunction() {
        assertThatThrownBy(() -> items.onItem().failWith((Function<? super Integer, ? extends Throwable>) null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mapper");

        assertThatThrownBy(() -> Uni.createFrom().item(1).onItem().failWith((Supplier<? extends Throwable>) null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("supplier");
    }

    @Test
    public void testSupplierNotCalledOnUpstreamFailure() {
        AtomicBoolean called = new AtomicBoolean();
        items.onItem().failWith((Supplier<Throwable>) TestException::new)
                .onItem().failWith(() -> {
                    called.set(true);
                    return new IOException();
                })
                .subscribe().withSubscriber(AssertSubscriber.<Number> create(1))
                .assertFailedWith(TestException.class, "");
        assertThat(called).isFalse();
        assertThat(items.isCancelled()).isTrue();
    }

    @Test
    public void testFunctionNotCalledOnUpstreamFailure() {
        AtomicBoolean called = new AtomicBoolean();
        items.onItem().failWith((Supplier<Throwable>) TestException::new)
                .onItem().failWith(item -> {
                    called.set(true);
                    return new IOException(Integer.toString(item));
                })
                .subscribe().withSubscriber(AssertSubscriber.<Number> create(1))
                .assertFailedWith(TestException.class, "");

        assertThat(called).isFalse();
        assertThat(items.isCancelled()).isTrue();
    }

    @Test
    public void testSupplierNotCalledOnCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        AssertSubscriber<Integer> subscriber = nothing
                .onItem().failWith(() -> {
                    called.set(true);
                    return new IOException();
                })
                .subscribe().withSubscriber(new AssertSubscriber<>(10));

        subscriber.assertSubscribed()
                .cancel()
                .assertNotTerminated();
        assertThat(called).isFalse();
        assertThat(nothing.isCancelled()).isTrue();
    }

    @Test
    public void testFunctionNotCalledOnCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        AssertSubscriber<Integer> subscriber = nothing
                .onItem().failWith(item -> {
                    called.set(true);
                    return new IOException();
                })
                .subscribe().withSubscriber(new AssertSubscriber<>(10));

        subscriber.assertSubscribed()
                .cancel()
                .assertNotTerminated();
        assertThat(called).isFalse();
        assertThat(nothing.isCancelled()).isTrue();
    }

    @Test
    public void testSupplierNotCalledOnImmediateCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        AssertSubscriber<Integer> subscriber = items
                .onItem().failWith(() -> {
                    called.set(true);
                    return new IOException();
                })
                .subscribe().withSubscriber(new AssertSubscriber<>(0, true));

        subscriber.assertSubscribed()
                .assertNotTerminated();
        assertThat(called).isFalse();
        assertThat(items.isCancelled()).isTrue();
    }

    @Test
    public void testFunctionNotCalledOnImmediateCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        AssertSubscriber<Integer> subscriber = items
                .onItem().failWith(item -> {
                    called.set(true);
                    return new IOException();
                })
                .subscribe().withSubscriber(new AssertSubscriber<>(0, true));

        subscriber.assertSubscribed()
                .assertNotTerminated();
        assertThat(called).isFalse();
        assertThat(items.isCancelled()).isTrue();
    }

    @Test
    public void testFailWithFunction() {
        items
                .onItem().failWith(i -> new TestException("boom-" + i))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(TestException.class, "boom-1");
        assertThat(items.isCancelled()).isTrue();
    }

    @Test
    public void testFailWithFunctionOnEmptyUpstream() {
        Multi.createFrom().empty()
                .onItem().failWith(i -> new TestException("boom-" + i))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted();
    }

    @Test
    public void testFailWithMapperOnEmptyUpstream() {
        Multi.createFrom().empty()
                .onItem().failWith(() -> new TestException("boom"))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted();
    }

    @Test
    public void testFailWithFunctionWithSubscriber() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> uni = items.onItem()
                .failWith(s -> new IOException(Integer.toString(s + count.getAndIncrement())));
        uni
                .subscribe().withSubscriber(AssertSubscriber.<Number> create(1))
                .assertFailedWith(IOException.class, "1");
        uni
                .subscribe().withSubscriber(AssertSubscriber.<Number> create(1))
                .assertFailedWith(IOException.class, "2");
    }

    @Test
    public void testFailWithSupplier() {
        items
                .onItem().failWith(() -> new TestException("boom"))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(TestException.class, "boom");
        assertThat(items.invocationCount()).isEqualTo(1);
        items
                .onItem().failWith(() -> new IOException("boom"))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
        assertThat(items.invocationCount()).isEqualTo(2);
    }

    @Test
    public void testSupplierOrFunctionReturningNull() {
        items.onItem().failWith(item -> null)
                .subscribe().withSubscriber(new AssertSubscriber<>(10))
                .assertFailedWith(NullPointerException.class, "mapper");

        items.onItem().failWith(() -> null).subscribe().withSubscriber(new AssertSubscriber<>(10))
                .assertFailedWith(NullPointerException.class, "supplier");
    }

    @Test
    public void testSupplierOrFunctionThrowingException() {
        items.onItem().failWith(item -> {
            throw new IllegalArgumentException("boom-" + item);
        }).subscribe().withSubscriber(new AssertSubscriber<>(10))
                .assertFailedWith(IllegalArgumentException.class, "boom");

        items.onItem().failWith(() -> {
            throw new IllegalArgumentException("boom");
        }).subscribe().withSubscriber(new AssertSubscriber<>(10))
                .assertFailedWith(IllegalArgumentException.class, "boom");
    }

}
