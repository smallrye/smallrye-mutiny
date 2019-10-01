package io.smallrye.reactive.operators;

import static io.smallrye.reactive.operators.MultiCollector.getFlowable;

import java.util.function.Consumer;

import io.smallrye.reactive.Multi;

public class Overflows {

    private Overflows() {
        // Avoid direct instantiation.
    }

    public static <T> Multi<T> buffer(Multi<T> upstream) {
        return new DefaultMulti<>(getFlowable(upstream).onBackpressureBuffer());
    }

    public static <T> Multi<T> buffer(Multi<T> upstream, int size) {
        return new DefaultMulti<>(getFlowable(upstream).onBackpressureBuffer(size));
    }

    public static <T> Multi<T> dropNewItems(Multi<T> upstream) {
        return new DefaultMulti<>(getFlowable(upstream).onBackpressureDrop());
    }

    public static <T> Multi<T> dropNewItems(Multi<T> upstream, Consumer<T> callback) {
        return new DefaultMulti<>(getFlowable(upstream).onBackpressureDrop(callback::accept));
    }

    public static <T> Multi<T> dropBufferedItems(Multi<T> upstream) {
        return new DefaultMulti<>(getFlowable(upstream).onBackpressureLatest());
    }

}
