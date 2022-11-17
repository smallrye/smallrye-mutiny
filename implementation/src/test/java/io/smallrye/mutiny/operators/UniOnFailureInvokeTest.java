package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

public class UniOnFailureInvokeTest {

    private static final IOException BOOM = new IOException("boom");
    private final Uni<Integer> failure = Uni.createFrom().failure(BOOM);

    @Test
    public void testThatConsumeMustNotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).onFailure().invoke((Consumer<Throwable>) null));
    }

    @Test
    public void testThatFunctionMustNotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).onFailure().call((Function<Throwable, Uni<?>>) null));
    }

    @Test
    public void testInvokeOnFailure() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        AtomicBoolean invokedRunnable = new AtomicBoolean();
        AtomicBoolean invokedUni = new AtomicBoolean();
        int res = failure
                .onFailure().invoke(container::set)
                .onFailure().invoke(() -> invokedRunnable.set(true))
                .onFailure().call(ignored -> {
                    invokedUni.set(true);
                    return Uni.createFrom().item(69);
                })
                .onFailure().recoverWithItem(1)
                .await().indefinitely();

        assertThat(res).isEqualTo(1);
        assertThat(invokedRunnable.get()).isTrue();
        assertThat(invokedUni.get()).isTrue();
        assertThat(container).hasValue(BOOM);
    }

    @Test
    public void testInvokeNotCalledOnItem() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        int res = Uni.createFrom().item(3)
                .onFailure().invoke(container::set)
                .onFailure().recoverWithItem(1)
                .await().indefinitely();

        assertThat(res).isEqualTo(3);
        assertThat(container).hasValue(null);
    }

    @Test
    public void testCallOnFailure() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        AtomicInteger called = new AtomicInteger(-1);
        AtomicBoolean calledSupplier = new AtomicBoolean();
        int res = failure
                .onFailure().call(t -> {
                    container.set(t);
                    return Uni.createFrom().item(22).onItem().invoke(called::set);
                })
                .onFailure().call(() -> {
                    calledSupplier.set(true);
                    return Uni.createFrom().item(69);
                })
                .onFailure().recoverWithItem(1)
                .await().indefinitely();

        assertThat(res).isEqualTo(1);
        assertThat(calledSupplier.get()).isTrue();
        assertThat(container).hasValue(BOOM);
        assertThat(called).hasValue(22);
    }

    @Test
    public void testCallNotCalledOnItem() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        AtomicInteger called = new AtomicInteger(-1);
        int res = Uni.createFrom().item(3)
                .onFailure().call(t -> {
                    container.set(t);
                    return Uni.createFrom().item(22).onItem().invoke(called::set);
                })
                .onFailure().recoverWithItem(1)
                .await().indefinitely();

        assertThat(res).isEqualTo(3);
        assertThat(container).hasValue(null);
        assertThat(called).hasValue(-1);
    }

    @Test
    public void testInvokeNotCalledOnNullItem() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        int res = Uni.createFrom().nullItem()
                .onFailure().invoke(container::set)
                .onFailure().recoverWithItem(1)
                .onItem().transform(x -> 5)
                .await().indefinitely();

        assertThat(res).isEqualTo(5);
        assertThat(container).hasValue(null);
    }

    @Test
    public void testCallNotCalledOnNullItem() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        AtomicInteger called = new AtomicInteger(-1);
        int res = Uni.createFrom().nullItem()
                .onFailure().call(t -> {
                    container.set(t);
                    return Uni.createFrom().item(22).onItem().invoke(called::set);
                })
                .onFailure().recoverWithItem(1)
                .onItem().transform(x -> 4)
                .await().indefinitely();

        assertThat(res).isEqualTo(4);
        assertThat(container).hasValue(null);
        assertThat(called).hasValue(-1);
    }

    @Test
    public void testInvokeOnFailureWithExceptionThrownByCallback() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        assertThatThrownBy(() -> failure
                .onFailure().invoke(t -> {
                    container.set(t);
                    throw new IllegalStateException("bad");
                })
                .await().indefinitely()).isInstanceOf(CompositeException.class).satisfies(t -> {
                    CompositeException ex = (CompositeException) t;
                    assertThat(ex.getCauses()).hasSize(2).contains(BOOM);
                });
    }

    @Test
    public void testCallOnFailureWithExceptionThrownByCallback() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        assertThatThrownBy(() -> failure
                .onFailure().call(t -> {
                    container.set(t);
                    throw new IllegalStateException("bad");
                })
                .await().indefinitely()).isInstanceOf(CompositeException.class).satisfies(t -> {
                    CompositeException ex = (CompositeException) t;
                    assertThat(ex.getCauses()).hasSize(2).contains(BOOM);
                });
    }

    @Test
    public void testCallOnFailureWithCallbackReturningNull() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        assertThatThrownBy(() -> failure
                .onFailure().call(t -> {
                    container.set(t);
                    return null;
                })
                .await().indefinitely()).isInstanceOf(CompositeException.class).satisfies(t -> {
                    CompositeException ex = (CompositeException) t;
                    assertThat(ex.getCauses()).hasSize(2).contains(BOOM);
                });
    }

    @Test
    public void testCancellationBeforeActionCompletes() {
        AtomicBoolean terminated = new AtomicBoolean();
        Uni<Object> uni = Uni.createFrom().emitter(e -> e.onTermination(() -> terminated.set(true)));

        AtomicInteger result = new AtomicInteger();
        AtomicReference<Throwable> failed = new AtomicReference<>();
        Cancellable cancellable = failure.onFailure().call(i -> uni).subscribe()
                .with(result::set, failed::set);

        cancellable.cancel();
        assertThat(result).hasValue(0);
        assertThat(failed).hasValue(null);
        assertThat(terminated).isTrue();
    }
}
