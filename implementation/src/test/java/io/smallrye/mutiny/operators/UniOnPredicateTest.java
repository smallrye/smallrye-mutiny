package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

@SuppressWarnings("ConstantConditions")
public class UniOnPredicateTest {

    @Test
    public void testTransform() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().when(Objects::nonNull).transform(String::toUpperCase)
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item("yo")
                .onItem().when("yolo"::equals).transform(String::toUpperCase)
                .await().indefinitely()).isNull();

        assertThat(Uni.createFrom().item("yo")
                .onItem().when("yolo"::equals).transform(String::toUpperCase)
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().when(Objects::nonNull).transform(String::toUpperCase)
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testTransformElse() {
        assertThat(Uni.createFrom().item("hElLo")
                .onItem().when("hElLo"::equals).transformElse(String::toUpperCase, String::toLowerCase)
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item("YolO")
                .onItem().when("yolo"::equals).transformElse(String::toUpperCase, String::toLowerCase)
                .await().indefinitely()).isEqualTo("yolo");

        assertThat(Uni.createFrom().item("YolO")
                .onItem().when("yolo"::equals).transformElse(String::toUpperCase, item -> item)
                .await().indefinitely()).isEqualTo("YolO");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().when("yolo"::equals).transformElse(String::toUpperCase, String::toLowerCase)
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testInvoke() {
        AtomicBoolean invoked = new AtomicBoolean();
        assertThat(Uni.createFrom().item("hello")
                .onItem().when(Objects::nonNull).invoke(s -> invoked.set(s.equals("hello")))
                .await().indefinitely()).isEqualTo("hello");
        assertThat(invoked).isTrue();

        invoked.set(false);
        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().when(Objects::nonNull).invoke(s -> invoked.set(true))
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).isEqualTo("yolo");
        assertThat(invoked).isFalse();

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().when(Objects::nonNull).invoke(s -> invoked.set(true))
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).hasMessageContaining("boom");

        assertThat(invoked).isFalse();
    }

    @Test
    public void testInvokeWithRunnable() {
        AtomicBoolean invoked = new AtomicBoolean();
        assertThat(Uni.createFrom().item("hello")
                .onItem().when(Objects::nonNull).invoke(() -> invoked.set(true))
                .await().indefinitely()).isEqualTo("hello");
        assertThat(invoked).isTrue();

        invoked.set(false);
        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().when(Objects::nonNull).invoke(() -> invoked.set(true))
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).isEqualTo("yolo");
        assertThat(invoked).isFalse();

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().when(Objects::nonNull).invoke(() -> invoked.set(true))
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).hasMessageContaining("boom");

        assertThat(invoked).isFalse();
    }

    @Test
    public void testCall() {
        AtomicBoolean invoked = new AtomicBoolean();
        AtomicReference<String> called = new AtomicReference<>();
        assertThat(Uni.createFrom().item("hello")
                .onItem().when(Objects::nonNull).call(s -> {
                    invoked.set(s.equals("hello"));
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .await().indefinitely()).isEqualTo("hello");
        assertThat(invoked).isTrue();
        assertThat(called).hasValue("something");

        invoked.set(false);
        called.set(null);
        assertThat(Uni.createFrom().nullItem()
                .onItem().when(Objects::nonNull).call(s -> {
                    invoked.set(s.equals("hello"));
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).isEqualTo("yolo");
        assertThat(invoked).isFalse();
        assertThat(called).hasValue(null);

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().when(Objects::nonNull).call(s -> {
                    invoked.set(s.equals("hello"));
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).hasMessageContaining("boom");

        assertThat(invoked).isFalse();
        assertThat(called).hasValue(null);
    }

    @Test
    public void testCallWithSupplier() {
        AtomicBoolean invoked = new AtomicBoolean();
        AtomicReference<String> called = new AtomicReference<>();
        assertThat(Uni.createFrom().item("hello")
                .onItem().when(Objects::nonNull).call(() -> {
                    invoked.set(true);
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .await().indefinitely()).isEqualTo("hello");
        assertThat(invoked).isTrue();
        assertThat(called).hasValue("something");

        invoked.set(false);
        called.set(null);
        assertThat(Uni.createFrom().nullItem()
                .onItem().when(Objects::nonNull).call(s -> {
                    invoked.set(true);
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).isEqualTo("yolo");
        assertThat(invoked).isFalse();
        assertThat(called).hasValue(null);

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().when(Objects::nonNull).call(s -> {
                    invoked.set(true);
                    return Uni.createFrom().item("something").onItem().invoke(called::set);
                })
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).hasMessageContaining("boom");

        assertThat(invoked).isFalse();
        assertThat(called).hasValue(null);
    }

    @Test
    public void testCallProducingNull() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> Uni.createFrom().item("hello")
                        .onItem().when(Objects::nonNull).call(s -> null)
                        .await().indefinitely());
    }

    @Test
    public void testCallProducingFailure() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> Uni.createFrom().item("hello")
                        .onItem().when(Objects::nonNull)
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
                .onItem().when(Objects::nonNull)
                .call(s -> emitter)
                .subscribe().with(res::set);

        cancellable.cancel();
        assertThat(res).hasValue(null);
        assertThat(called).isTrue();
    }

    @Test
    public void testProduceUni() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().when(Objects::nonNull).transformToUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().when(Objects::nonNull).transformToUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().when(Objects::nonNull).transformToUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testProduceUniElse() {
        assertThat(Uni.createFrom().item("hElLo")
                .onItem().when("hElLo"::equals).transformToUniElse(
                        s -> Uni.createFrom().item(s.toUpperCase()),
                        s -> Uni.createFrom().item(s.toLowerCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().when(Objects::nonNull).transformToUniElse(
                        s -> Uni.createFrom().item(s.toUpperCase()),
                        s -> Uni.createFrom().item("yolo"))
                .await().indefinitely()).isEqualTo("yolo");

        assertThat(Uni.createFrom().item("YolO")
                .onItem().when("yolo"::equals).transformToUniElse(
                        s -> Uni.createFrom().item(s.toUpperCase()),
                        s -> Uni.createFrom().item(s))
                .await().indefinitely()).isEqualTo("YolO");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().when(Objects::nonNull).transformToUniElse(
                        s -> Uni.createFrom().item(s.toUpperCase()),
                        s -> Uni.createFrom().item(s.toLowerCase()))
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testTransformToUniWithEmitter() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().when(Objects::nonNull).transformToUni((s, e) -> e.complete(s.toUpperCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().when(Objects::nonNull).transformToUni((s, e) -> e.complete(s.toUpperCase()))
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().when(Objects::nonNull).transformToUni((s, e) -> e.complete(s.toUpperCase()))
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testTransformToUniWithEmitterElse() {
        assertThat(Uni.createFrom().item("hElLo")
                .onItem().when("hElLo"::equals).transformToUniElse(
                        (s, e) -> e.complete(s.toUpperCase()),
                        (s, e) -> e.complete(s.toLowerCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().when(Objects::nonNull).transformToUniElse(
                        (s, e) -> e.complete(s.toUpperCase()),
                        (s, e) -> e.complete("yolo"))
                .await().indefinitely()).isEqualTo("yolo");

        assertThat(Uni.createFrom().item("YolO")
                .onItem().when("yolo"::equals).transformToUniElse(
                        (s, e) -> e.complete(s.toUpperCase()),
                        (s, e) -> e.complete(s))
                .await().indefinitely()).isEqualTo("YolO");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().when(Objects::nonNull).transformToUniElse(
                        (s, e) -> e.complete(s.toUpperCase()),
                        (s, e) -> e.complete("yolo"))
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testTransformToMulti() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().when(Objects::nonNull).transformToMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collect().first()
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().when(Objects::nonNull).transformToMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collect().first()
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().when(Objects::nonNull).transformToMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collect().first()
                .onItem().when(Objects::isNull).transform(ignore -> "yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testTransformToMultiElse() {
        assertThat(Uni.createFrom().item("hElLo")
                .onItem().when("hElLo"::equals).transformToMultiElse(
                        x -> Multi.createFrom().item(x.toUpperCase()),
                        x -> Multi.createFrom().item(x.toLowerCase()))
                .collect().first()
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().when(Objects::nonNull).transformToMultiElse(
                        x -> Multi.createFrom().item(x.toUpperCase()),
                        x -> Multi.createFrom().item("yolo"))
                .collect().first()
                .await().indefinitely()).isEqualTo("yolo");

        assertThat(Uni.createFrom().item("YolO")
                .onItem().when("yolo"::equals).transformToMultiElse(
                        x -> Multi.createFrom().item(x.toUpperCase()),
                        x -> Multi.createFrom().item(x))
                .collect().first()
                .await().indefinitely()).isEqualTo("YolO");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().when(Objects::nonNull).transformToMultiElse(
                        x -> Multi.createFrom().item(x.toUpperCase()),
                        x -> Multi.createFrom().item("yolo"))
                .collect().first()
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testFailWithExceptionSupplier() {
        assertThrows(IllegalStateException.class,
                () -> Uni.createFrom().item("hello")
                        .onItem().when(Objects::nonNull).failWith(() -> new IllegalStateException("boom"))
                        .await().indefinitely());
    }

    @Test
    public void testFailWithExceptionSupplierNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item("hello")
                        .onItem().when(Objects::nonNull).failWith((Supplier<Throwable>) null)
                        .await().indefinitely());
    }

    @Test
    public void testFailWithExceptionNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item("hello")
                        .onItem().when(Objects::nonNull).failWith((Exception) null)
                        .await().indefinitely());
    }

    @Test
    public void testFailWithException() {
        assertThrows(RuntimeException.class,
                () -> Uni.createFrom().item((Object) null).onItem().when(Objects::isNull).failWith(new RuntimeException("boom"))
                        .await()
                        .indefinitely());
    }

    @Test
    public void testFailWithExceptionSetToNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Uni.createFrom().item((Object) null).onItem().when(Objects::isNull).failWith((Exception) null).await()
                        .indefinitely());
    }

    @Test
    public void testFailWithExceptionNotCalledOnItem() {
        assertThat(Uni.createFrom().item(1).onItem().when(Objects::isNull).failWith(new IOException("boom")).await()
                .indefinitely())
                .isEqualTo(1);
    }

    @Test
    public void estFailWithExceptionSupplierThrowingException() {
        assertThrows(IllegalStateException.class,
                () -> Uni.createFrom().item((Object) null).onItem().when(Objects::isNull).failWith(() -> {
                    throw new IllegalStateException("boom");
                }).await().indefinitely());
    }

    @Test
    public void testFailWithExceptionSupplierNotCalledOnItem() {
        assertThat(Uni.createFrom().item(1).onItem().when(Objects::isNull).failWith(new IOException("boom")).await()
                .indefinitely())
                .isEqualTo(1);
    }
}
