package io.smallrye.mutiny.helpers;

/**
 * The infamous sneaky throw helper.
 */
public interface SneakyThrow {

    @SuppressWarnings("unchecked")
    static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
