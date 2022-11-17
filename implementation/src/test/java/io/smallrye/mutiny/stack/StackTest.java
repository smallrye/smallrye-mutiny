package io.smallrye.mutiny.stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class StackTest {

    Random random = new Random();

    @Test
    public void testWithUni() {
        int length = 10_000_000;
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);

        List<Integer> results = new ArrayList<>();
        Multi.createFrom().items(() -> intStream(bytes).boxed())
                .onItem().transformToUni(i -> Uni.createFrom().item(i)).concatenate()
                .subscribe().with(results::add, Throwable::printStackTrace);

        for (int i = 0; i < length; i++) {
            Assertions.assertThat(bytes[i]).isEqualTo(results.get(i).byteValue());
        }

    }

    @Test
    public void testWithMulti() {
        int length = 10_000_000;
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);

        List<Integer> results = new ArrayList<>();
        Multi.createFrom().items(() -> intStream(bytes).boxed())
                .onItem().transformToMultiAndConcatenate(i -> Uni.createFrom().item(i).toMulti())
                .subscribe().with(results::add, Throwable::printStackTrace);

        for (int i = 0; i < length; i++) {
            Assertions.assertThat(bytes[i]).isEqualTo(results.get(i).byteValue());
        }
    }

    @Test
    public void testWithRepeat() {
        int length = 10_000_000;
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);

        List<Integer> list = new ArrayList<>();

        AtomicInteger index = new AtomicInteger();
        Uni.createFrom().<Integer> emitter(e -> {
            if (index.get() > bytes.length - 1) {
                e.complete(Integer.MIN_VALUE);
            } else {
                int i = index.getAndIncrement();
                e.complete(Byte.valueOf(bytes[i]).intValue());
            }
        }).repeat().until(b -> b == Integer.MIN_VALUE)
                .subscribe().with(list::add);

        for (int i = 0; i < length; i++) {
            Assertions.assertThat(bytes[i]).isEqualTo(list.get(i).byteValue());
        }
    }

    public static IntStream intStream(byte[] array) {
        return IntStream.range(0, array.length).map(idx -> array[idx]);
    }

}
