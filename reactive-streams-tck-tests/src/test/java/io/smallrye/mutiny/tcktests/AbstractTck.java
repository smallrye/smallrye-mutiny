package io.smallrye.mutiny.tcktests;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import io.smallrye.mutiny.Multi;

public class AbstractTck {

    /**
     * An infinite stream of integers starting from one.
     */
    Multi<Integer> infiniteStream() {
        return Multi.createFrom().iterable(() -> {
            AtomicInteger value = new AtomicInteger();
            return IntStream.generate(value::incrementAndGet).boxed().iterator();
        });
    }

}
