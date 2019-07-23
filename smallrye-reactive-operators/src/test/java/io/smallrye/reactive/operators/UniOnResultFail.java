package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class UniOnResultFail {

    private Uni<Integer> one = Uni.createFrom().result(1);

    @Test(expected = IllegalArgumentException.class)
    public void testThatMapperCannotBeNull() {
        one.onResult().failWith(null);
    }

    @Test
    public void testMapToException() {
        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = one.onResult().failWith(s -> new IOException(Integer.toString(s + count.getAndIncrement())));
        uni
                .subscribe().withSubscriber(AssertSubscriber.<Number>create())
                .assertFailure(IOException.class, "1");
        uni
                .subscribe().withSubscriber(AssertSubscriber.<Number>create())
                .assertFailure(IOException.class, "2");
    }

}
