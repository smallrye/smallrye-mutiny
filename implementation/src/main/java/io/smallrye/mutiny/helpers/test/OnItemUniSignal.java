package io.smallrye.mutiny.helpers.test;

/**
 * A onItem signal.
 */
public final class OnItemUniSignal<T> implements UniSignal {
    private final T item;

    public OnItemUniSignal(T item) {
        this.item = item;
    }

    @Override
    public T value() {
        return item;
    }

    @Override
    public String toString() {
        return "OnItemSignal{" +
                "item=" + item +
                '}';
    }
}
