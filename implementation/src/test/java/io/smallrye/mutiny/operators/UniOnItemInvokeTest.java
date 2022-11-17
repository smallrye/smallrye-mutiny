package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

public class UniOnItemInvokeTest {

    private final Uni<Integer> one = Uni.createFrom().item(1);
    private final Uni<Integer> failed = Uni.createFrom().failure(new IOException("boom"));
    private final Uni<Integer> two = Uni.createFrom().item(2);

    @Test
    public void testThatConsumerMustNotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).onItem().invoke((Consumer<? super Integer>) null));
    }

    @Test
    public void testThatMapperMustNotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).onItem().call((Function<? super Integer, Uni<?>>) null));
    }

    @Test
    public void testThatConsumerMustNotBeNullWithShortcut() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().item(1).invoke((Consumer<? super Integer>) null));
    }

    @Test
    public void testThatMapperMustNotBeNullWithShortcut() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item(1).call((Function<? super Integer, Uni<?>>) null));
    }

    @Test
    public void testInvokeOnItem() {
        AtomicInteger res = new AtomicInteger();

        int r = one.onItem().invoke(res::set)
                .await().indefinitely();

        assertThat(r).isEqualTo(1);
        assertThat(res).hasValue(1);
    }

    @Test
    public void testInvokeOnItemWithRunnable() {
        AtomicBoolean called = new AtomicBoolean();

        int r = one.onItem().invoke(() -> called.set(true))
                .await().indefinitely();

        assertThat(r).isEqualTo(1);
        assertThat(called).isTrue();
    }

    @Test
    public void testInvokeOnItemWithShortcut() {
        AtomicInteger res = new AtomicInteger();

        int r = one.invoke(res::set)
                .await().indefinitely();

        assertThat(r).isEqualTo(1);
        assertThat(res).hasValue(1);
    }

    @Test
    public void testInvokeOnItemWithRunnableShortcut() {
        AtomicInteger res = new AtomicInteger();

        int r = one.invoke(res::incrementAndGet)
                .await().indefinitely();

        assertThat(r).isEqualTo(1);
        assertThat(res).hasValue(1);
    }

    @Test
    public void testInvokeOnFailure() {
        AtomicInteger res = new AtomicInteger(-1);

        assertThatThrownBy(() -> failed.onItem().invoke(res::set).await().indefinitely())
                .isInstanceOf(CompletionException.class).hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("boom");

        assertThat(res).hasValue(-1);
    }

    @Test
    public void testFailureInCallback() {
        AtomicInteger res = new AtomicInteger();

        assertThatThrownBy(() -> one.onItem().invoke(i -> {
            res.set(i);
            throw new RuntimeException("boom");
        }).await().indefinitely()).isInstanceOf(RuntimeException.class).hasMessageContaining("boom");

        assertThat(res).hasValue(1);
    }

    @Test
    public void testCallOnItem() {
        AtomicInteger res = new AtomicInteger();
        AtomicInteger twoGotCalled = new AtomicInteger();

        int r = one.onItem().call(i -> {
            res.set(i);
            return two.onItem().invoke(twoGotCalled::set);
        })
                .await().indefinitely();

        assertThat(r).isEqualTo(1);
        assertThat(twoGotCalled).hasValue(2);
        assertThat(res).hasValue(1);
    }

    @Test
    public void testCallOnItemWithSupplier() {
        AtomicInteger twoGotCalled = new AtomicInteger();

        int r = one.onItem().call(() -> two.onItem().invoke(twoGotCalled::set))
                .await().indefinitely();

        assertThat(r).isEqualTo(1);
        assertThat(twoGotCalled).hasValue(2);
    }

    @Test
    public void testCallOnItemWithShortcut() {
        AtomicInteger res = new AtomicInteger();
        AtomicInteger twoGotCalled = new AtomicInteger();

        int r = one.call(i -> {
            res.set(i);
            return two.invoke(twoGotCalled::set);
        })
                .await().indefinitely();

        assertThat(r).isEqualTo(1);
        assertThat(twoGotCalled).hasValue(2);
        assertThat(res).hasValue(1);
    }

    @Test
    public void testCallOnItemWithSupplierShortcut() {
        AtomicInteger res = new AtomicInteger();
        AtomicInteger twoGotCalled = new AtomicInteger();

        int r = one.call(() -> {
            res.set(23);
            return two.invoke(twoGotCalled::set);
        })
                .await().indefinitely();

        assertThat(r).isEqualTo(1);
        assertThat(twoGotCalled).hasValue(2);
        assertThat(res).hasValue(23);
    }

    @Test
    public void testDeprecatedInvokeUniOnFailure() {
        AtomicInteger res = new AtomicInteger(-1);
        AtomicInteger twoGotCalled = new AtomicInteger(-1);

        assertThatThrownBy(() -> failed.onItem().call(i -> {
            res.set(i);
            return two.onItem().invoke(twoGotCalled::set);
        }).await().indefinitely()).isInstanceOf(CompletionException.class).hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("boom");

        assertThat(twoGotCalled).hasValue(-1);
        assertThat(res).hasValue(-1);
    }

    @Test
    public void testCallOnFailure() {
        AtomicInteger res = new AtomicInteger(-1);
        AtomicInteger twoGotCalled = new AtomicInteger(-1);

        assertThatThrownBy(() -> failed.onItem().call(i -> {
            res.set(i);
            return two.onItem().invoke(twoGotCalled::set);
        }).await().indefinitely()).isInstanceOf(CompletionException.class).hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("boom");

        assertThat(twoGotCalled).hasValue(-1);
        assertThat(res).hasValue(-1);
    }

    @Test
    public void testFailureInAsyncCallback() {
        AtomicInteger res = new AtomicInteger();

        assertThatThrownBy(() -> one.onItem().call(i -> {
            res.set(i);
            throw new RuntimeException("boom");
        }).await().indefinitely()).isInstanceOf(RuntimeException.class).hasMessageContaining("boom");

        assertThat(res).hasValue(1);
    }

    @Test
    public void testNullReturnedByAsyncCallback() {
        AtomicInteger res = new AtomicInteger();

        assertThatThrownBy(() -> one.onItem().call(i -> {
            res.set(i);
            return null;
        }).await().indefinitely()).isInstanceOf(NullPointerException.class).hasMessageContaining("null");

        assertThat(res).hasValue(1);
    }

    @Test
    public void testCallWithSubFailure() {
        AtomicInteger res = new AtomicInteger(-1);
        AtomicInteger twoGotCalled = new AtomicInteger(-1);

        assertThatThrownBy(() -> one.onItem().call(i -> {
            res.set(i);
            return two.onItem().invoke(twoGotCalled::set)
                    .onItem().failWith(k -> new IllegalStateException("boom-" + k));
        }).await().indefinitely()).isInstanceOf(IllegalStateException.class).hasMessageContaining("boom-2");

        assertThat(twoGotCalled).hasValue(2);
        assertThat(res).hasValue(1);
    }

    @Test
    public void testCancellationBeforeActionCompletes() {
        AtomicBoolean terminated = new AtomicBoolean();
        Uni<Object> uni = Uni.createFrom().emitter(e -> e.onTermination(() -> terminated.set(true)));

        AtomicInteger result = new AtomicInteger();
        Cancellable cancellable = one.onItem().call(i -> uni).subscribe().with(result::set);

        cancellable.cancel();
        assertThat(result).hasValue(0);
        assertThat(terminated).isTrue();
    }

}
