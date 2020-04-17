package io.smallrye.mutiny.unchecked;

import java.util.function.Supplier;

/**
 * Represents a supplier of items.
 * <p>
 * The supplier can throw {@link Exception Exceptions}.
 *
 * @param <T> the type of items supplied by this supplier
 */
@FunctionalInterface
public interface UncheckedSupplier<T> {

    /**
     * Creates a new {@link UncheckedSupplier} from an existing {@link Supplier}
     *
     * @param supplier the supplier
     * @param <T> the type of items supplied by this supplier
     * @return the new {@link UncheckedSupplier}
     */
    static <T> UncheckedSupplier<T> from(Supplier<T> supplier) {
        return supplier::get;
    }

    /**
     * Gets an item.
     *
     * @return an item
     * @throws Exception if anything wrong happen
     */
    T get() throws Exception;

    /**
     * @return the {@link Supplier} getting the item produced by this {@link UncheckedSupplier}. If an exception is
     *         thrown during the production, this exception is rethrown, wrapped into a {@link RuntimeException} if needed.
     */
    default Supplier<T> toSupplier() {
        return () -> {
            try {
                return get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

}
