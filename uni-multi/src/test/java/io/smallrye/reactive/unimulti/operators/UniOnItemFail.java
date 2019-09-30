package io.smallrye.reactive.unimulti.operators;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.smallrye.reactive.unimulti.Uni;

public class UniOnItemFail {

    private Uni<Integer> one = Uni.createFrom().item(1);

    @Test(expected = IllegalArgumentException.class)
    public void testThatMapperCannotBeNull() {
        one.onItem().failWith(null);
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
