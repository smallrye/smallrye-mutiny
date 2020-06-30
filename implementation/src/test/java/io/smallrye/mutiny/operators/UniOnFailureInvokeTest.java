package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

public class UniOnFailureInvokeTest {

    private static final IOException BOOM = new IOException("boom");
    private final Uni<Integer> failure = Uni.createFrom().failure(BOOM);

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatConsumeMustNotBeNull() {
        Uni.createFrom().item(1).onFailure().invoke(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFunctionMustNotBeNull() {
        Uni.createFrom().item(1).onFailure().invokeUni(null);
    }

    @Test
    public void testInvokeOnFailure() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        int res = failure
                .onFailure().invoke(container::set)
                .onFailure().recoverWithItem(1)
                .await().indefinitely();

        assertThat(res).isEqualTo(1);
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
    public void testInvokeUniOnFailure() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        AtomicInteger called = new AtomicInteger(-1);
        int res = failure
                .onFailure().invokeUni(t -> {
                    container.set(t);
                    return Uni.createFrom().item(22).onItem().invoke(called::set);
                })
                .onFailure().recoverWithItem(1)
                .await().indefinitely();

        assertThat(res).isEqualTo(1);
        assertThat(container).hasValue(BOOM);
        assertThat(called).hasValue(22);
    }

    @Test
    public void testInvokeUniNotCalledOnItem() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        AtomicInteger called = new AtomicInteger(-1);
        int res = Uni.createFrom().item(3)
                .onFailure().invokeUni(t -> {
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
    public void testInvokeUniNotCalledOnNullItem() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        AtomicInteger called = new AtomicInteger(-1);
        int res = Uni.createFrom().nullItem()
                .onFailure().invokeUni(t -> {
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
    public void testInvokeUniOnFailureWithExceptionThrownByCallback() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        assertThatThrownBy(() -> failure
                .onFailure().invokeUni(t -> {
                    container.set(t);
                    throw new IllegalStateException("bad");
                })
                .await().indefinitely()).isInstanceOf(CompositeException.class).satisfies(t -> {
                    CompositeException ex = (CompositeException) t;
                    assertThat(ex.getCauses()).hasSize(2).contains(BOOM);
                });
    }

    @Test
    public void testInvokeUniOnFailureWithCallbackReturningNull() {
        AtomicReference<Throwable> container = new AtomicReference<>();
        assertThatThrownBy(() -> failure
                .onFailure().invokeUni(t -> {
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
        Cancellable cancellable = failure.onFailure().invokeUni(i -> uni).subscribe()
                .with(result::set, failed::set);

        cancellable.cancel();
        assertThat(result).hasValue(0);
        assertThat(failed).hasValue(null);
        //noinspection ConstantConditions
        assertThat(terminated).isTrue();
    }
}
