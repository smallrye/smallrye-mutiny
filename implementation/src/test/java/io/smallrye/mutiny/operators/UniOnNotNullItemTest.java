package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

public class UniOnNotNullItemTest {

    @Test
    public void testTransform() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().transform(String::toUpperCase)
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().transform(String::toUpperCase)
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().transform(String::toUpperCase)
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testInvoke() {
        AtomicBoolean invoked = new AtomicBoolean();
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().invoke(s -> invoked.set(s.equals("hello")))
                .await().indefinitely()).isEqualTo("hello");
        assertThat(invoked).isTrue();

        invoked.set(false);
        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().invoke(s -> invoked.set(true))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");
        assertThat(invoked).isFalse();

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().invoke(s -> invoked.set(true))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");

        assertThat(invoked).isFalse();
    }

    @Test
    public void testInvokeWithRunnable() {
        AtomicBoolean invoked = new AtomicBoolean();
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().invoke(() -> invoked.set(true))
                .await().indefinitely()).isEqualTo("hello");
        assertThat(invoked).isTrue();

        invoked.set(false);
        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().invoke(() -> invoked.set(true))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");
        assertThat(invoked).isFalse();

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().invoke(() -> invoked.set(true))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");

        assertThat(invoked).isFalse();
    }

    @Test
    public void testCall() {
        AtomicBoolean invoked = new AtomicBoolean();
        AtomicReference<String> called = new AtomicReference<>();
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().call(s -> {
                    invoked.set(s.equals("hello"));
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .await().indefinitely()).isEqualTo("hello");
        assertThat(invoked).isTrue();
        assertThat(called).hasValue("something");

        invoked.set(false);
        called.set(null);
        assertThat(Uni.createFrom().nullItem()
                .onItem().ifNotNull().call(s -> {
                    invoked.set(s.equals("hello"));
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");
        assertThat(invoked).isFalse();
        assertThat(called).hasValue(null);

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().call(s -> {
                    invoked.set(s.equals("hello"));
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");

        assertThat(invoked).isFalse();
        assertThat(called).hasValue(null);
    }

    @Test
    public void testCallWithSupplier() {
        AtomicBoolean invoked = new AtomicBoolean();
        AtomicReference<String> called = new AtomicReference<>();
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().call(() -> {
                    invoked.set(true);
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .await().indefinitely()).isEqualTo("hello");
        assertThat(invoked).isTrue();
        assertThat(called).hasValue("something");

        invoked.set(false);
        called.set(null);
        assertThat(Uni.createFrom().nullItem()
                .onItem().ifNotNull().call(s -> {
                    invoked.set(true);
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");
        assertThat(invoked).isFalse();
        assertThat(called).hasValue(null);

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().call(s -> {
                    invoked.set(true);
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");

        assertThat(invoked).isFalse();
        assertThat(called).hasValue(null);
    }

    @Test
    public void testCallProducingNull() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> Uni.createFrom().item("hello")
                        .onItem().ifNotNull().call(s -> null)
                        .await().indefinitely());
    }

    @Test
    public void testCallProducingFailure() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> Uni.createFrom().item("hello")
                        .onItem().ifNotNull()
                        .call(s -> Uni.createFrom().failure(new IllegalStateException("boom")))
                        .await().indefinitely())
                .withMessageContaining("boom");
    }

    @Test
    public void testCallWithCancellationBeforeEmission() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicReference<String> res = new AtomicReference<>();
        Uni<Object> emitter = Uni.createFrom().emitter(e -> e.onTermination(() -> called.set(true)));

        Cancellable cancellable = Uni.createFrom().item("hello")
                .onItem().ifNotNull()
                .call(s -> emitter)
                .subscribe().with(res::set);

        cancellable.cancel();
        assertThat(res).hasValue(null);
        assertThat(called).isTrue();
    }

    @Test
    public void testProduceUni() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().transformToUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().transformToUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().transformToUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testTransformToUniWithEmitter() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().transformToUni((s, e) -> e.complete(s.toUpperCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().transformToUni((s, e) -> e.complete(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().transformToUni((s, e) -> e.complete(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testTransformToMulti() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().transformToMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collect().first()
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().transformToMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collect().first()
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().transformToMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collect().first()
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testFailWithExceptionSupplier() {
        assertThrows(IllegalStateException.class,
                () -> Uni.createFrom().item("hello")
                        .onItem().ifNotNull().failWith(() -> new IllegalStateException("boom"))
                        .await().indefinitely());
    }

    @Test
    public void testFailWithException() {
        assertThrows(IllegalStateException.class,
                () -> Uni.createFrom().item("hello")
                        .onItem().ifNotNull().failWith(new IllegalStateException("boom"))
                        .await().indefinitely());
    }

    @Test
    public void testFailWithExceptionSupplierNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item("hello")
                        .onItem().ifNotNull().failWith((Supplier<Throwable>) null)
                        .await().indefinitely());
    }

    @Test
    public void testFailWithExceptionNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item("hello")
                        .onItem().ifNotNull().failWith((Exception) null)
                        .await().indefinitely());
    }
}
