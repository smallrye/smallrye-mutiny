package io.smallrye.mutiny.operators;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

public class UniOnItemFailTest {

    private final Uni<Integer> one = Uni.createFrom().item(1);

    @Test
    public void testThatMapperCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> one.onItem().failWith(null));
    }

    @Test
    public void testMapToException() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = one.onItem().failWith(s -> new IOException(Integer.toString(s + count.getAndIncrement())));
        uni
                .subscribe().withSubscriber(UniAssertSubscriber.<Number> create())
                .assertFailure(IOException.class, "1");
        uni
                .subscribe().withSubscriber(UniAssertSubscriber.<Number> create())
                .assertFailure(IOException.class, "2");
    }

}
