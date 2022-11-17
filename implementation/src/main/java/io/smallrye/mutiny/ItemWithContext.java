package io.smallrye.mutiny;

import java.util.Objects;

/**
 * Models an item flowing along a Mutiny pipeline with its subscriber context attached.
 *
 * @param <T> the iten type
 * @see Uni#attachContext()
 * @see Multi#attachContext()
 */
public final class ItemWithContext<T> {

    private final Context context;
    private final T item;

    /**
     * Creates a new item with a context.
     * <p>
     * Since instances are being created by Mutiny operators there is no {@code null} check on parameters
     * for performance reasons.
     *
     * @param context the context
     * @param item the item
     */
    public ItemWithContext(Context context, T item) {
        this.context = context;
        this.item = item;
    }

    /**
     * Gives the context.
     *
     * @return the context
     */
    public Context context() {
        return context;
    }

    /**
     * Gives the item.
     *
     * @return the item
     */
    public T get() {
        return item;
    }

    @Override
    public String toString() {
        return "ItemWithContext{" +
                "context=" + context +
                ", item=" + item +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ItemWithContext<?> that = (ItemWithContext<?>) o;
        return Objects.equals(context, that.context) && Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, item);
    }
}
