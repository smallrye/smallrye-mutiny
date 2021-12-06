package io.smallrye.mutiny.unchecked;

import static io.smallrye.mutiny.unchecked.Unchecked.consumer;
import static io.smallrye.mutiny.unchecked.Unchecked.unchecked;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;

public class UncheckedConsumerTest {

    @Test
    public void testUncheckedConsumer() {
        AtomicInteger result = new AtomicInteger(-1);
        Consumer<Integer> consumer = consumer(result::set);
        Consumer<Integer> consumerFailingWithIo = consumer(x -> {
            throw new IOException("boom");
        });
        Consumer<Integer> consumerFailingWithArithmetic = consumer(x -> {
            throw new ArithmeticException("boom");
        });
        consumer.accept(1);
        assertThat(result).hasValue(1);
        assertThatThrownBy(() -> consumerFailingWithIo.accept(2))
                .hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");
        assertThatThrownBy(() -> consumerFailingWithArithmetic.accept(3))
                .isInstanceOf(ArithmeticException.class).hasMessageContaining("boom");
    }

    @Test
    public void testUncheckedBiConsumer() {
        AtomicInteger result = new AtomicInteger(-1);
        BiConsumer<Integer, Integer> consumer = consumer((i, j) -> result.set(i + j));
        BiConsumer<Integer, Integer> consumerFailingWithIo = consumer((x, y) -> {
            throw new IOException("boom");
        });
        BiConsumer<Integer, Integer> consumerFailingWithArithmetic = consumer((x, y) -> {
            throw new ArithmeticException("boom");
        });
        consumer.accept(1, 1);
        assertThat(result).hasValue(2);
        assertThatThrownBy(() -> consumerFailingWithIo.accept(2, 2))
                .hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");
        assertThatThrownBy(() -> consumerFailingWithArithmetic.accept(3, 3))
                .isInstanceOf(ArithmeticException.class).hasMessageContaining("boom");
    }

    @Test
    public void testChaining() throws Exception {
        unchecked(x -> {
        }).andThen(i -> {
        }).accept(1);
        unchecked((x, y) -> {
        }).andThen((i, j) -> {
        }).accept(1, 2);

        assertThatThrownBy(() -> unchecked(x -> {
        }).andThen(i -> {
            throw new IllegalStateException("boom");
        }).accept(1)).isInstanceOf(IllegalStateException.class).hasMessageContaining("boom");

        assertThatThrownBy(() -> unchecked((x, y) -> {
        }).andThen((i, j) -> {
            throw new IllegalStateException("boom");
        }).accept(1, 2)).isInstanceOf(IllegalStateException.class).hasMessageContaining("boom");

    }

    @Test
    public void testSubscription() {
        AtomicReference<String> reference = new AtomicReference<>();
        Multi.createFrom().item("hey").subscribe().with(consumer(s -> {
            TimeUnit.MILLISECONDS.sleep(100);
            reference.set(s);
        }));

        await().until(() -> reference.get() != null);
        assertThat(reference).hasValue("hey");
    }

}
