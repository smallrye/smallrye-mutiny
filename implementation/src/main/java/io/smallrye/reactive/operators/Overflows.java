package io.smallrye.reactive.operators;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.multi.overflow.MultiOnOverflowBufferOp;
import io.smallrye.reactive.operators.multi.overflow.MultiOnOverflowDropItemsOp;
import io.smallrye.reactive.operators.multi.overflow.MultiOnOverflowKeepLastOp;

import java.util.function.Consumer;

public class Overflows {

    private Overflows() {
        // Avoid direct instantiation.
    }

    public static <T> Multi<T> buffer(Multi<T> upstream) {
        return new MultiOnOverflowBufferOp<>(upstream, 128,
                true, false, x -> {
        });
    }

    public static <T> Multi<T> buffer(Multi<T> upstream, int size) {
        return new MultiOnOverflowBufferOp<>(upstream, size,
                false, false, x -> {
        });
    }

    public static <T> Multi<T> dropNewItems(Multi<T> upstream) {
        return new MultiOnOverflowDropItemsOp<>(upstream);
    }

    public static <T> Multi<T> dropNewItems(Multi<T> upstream, Consumer<T> callback) {
        return new MultiOnOverflowDropItemsOp<>(upstream, callback);
    }

    public static <T> Multi<T> keepLastItem(Multi<T> upstream) {
        return new MultiOnOverflowKeepLastOp<>(upstream);
    }

}
