package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniOnNotNullItemTest {

    @Test
    public void testApply() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().apply(String::toUpperCase)
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().apply(String::toUpperCase)
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().apply(String::toUpperCase)
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testInvoke() {
        AtomicBoolean invoked = new AtomicBoolean();
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().invoke(s -> {
                    invoked.set(s.equals("hello"));
                })
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
    public void testProduceUni() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().produceUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().produceUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().produceUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testProduceUniWithEmitter() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().produceUni((s, e) -> e.complete(s.toUpperCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().produceUni((s, e) -> e.complete(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().produceUni((s, e) -> e.complete(s.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testProduceCompletionStage() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().produceCompletionStage(x -> CompletableFuture.completedFuture(x.toUpperCase()))
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().produceCompletionStage(x -> CompletableFuture.completedFuture(x.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().produceCompletionStage(x -> CompletableFuture.completedFuture(x.toUpperCase()))
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }

    @Test
    public void testProduceMulti() {
        assertThat(Uni.createFrom().item("hello")
                .onItem().ifNotNull().produceMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collectItems().first()
                .await().indefinitely()).isEqualTo("HELLO");

        assertThat(Uni.createFrom().item(() -> (String) null)
                .onItem().ifNotNull().produceMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collectItems().first()
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).isEqualTo("yolo");

        assertThatThrownBy(() -> Uni.createFrom().<String> failure(new Exception("boom"))
                .onItem().ifNotNull().produceMulti(x -> Multi.createFrom().item(x.toUpperCase()))
                .collectItems().first()
                .onItem().ifNull().continueWith("yolo")
                .await().indefinitely()).hasMessageContaining("boom");
    }
}
