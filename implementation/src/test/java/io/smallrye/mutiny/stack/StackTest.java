package io.smallrye.mutiny.stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

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
                .onItem().produceUni(i -> Uni.createFrom().item(i)).concatenate()
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
                .onItem().produceMulti(i -> Uni.createFrom().item(i).toMulti()).concatenate()
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
        Uni.createFrom().<Byte> emitter(e -> {
            if (index.get() > bytes.length) {
                e.complete(null);
            } else {
                int i = index.getAndIncrement();
                e.complete(bytes[i]);
            }
        }).repeat().until(Objects::isNull)
                .subscribe().with(i -> list.add(i.intValue()));

        for (int i = 0; i < length; i++) {
            Assertions.assertThat(bytes[i]).isEqualTo(list.get(i).byteValue());
        }
    }

    public static IntStream intStream(byte[] array) {
        return IntStream.range(0, array.length).map(idx -> array[idx]);
    }

}
