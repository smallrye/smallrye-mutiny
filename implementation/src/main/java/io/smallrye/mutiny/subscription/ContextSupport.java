package io.smallrye.mutiny.subscription;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Context;

/**
 * Interface for subscribers and types that provide a {@link Context}.
 */
@Experimental("Context support is a new experimental API introduced in Mutiny 1.3.0")
public interface ContextSupport {

    /**
     * Provide a context.
     * <p>
     * Since calls to this method shall only be triggered when a Mutiny pipeline uses a {@code withContext} operator,
     * there is no need in general for caching the context value in a field of the implementing class.
     * Exceptions include operators that have cross-subscriber semantics such as memoizers or broadcasters.
     * <p>
     * This method is expected to be called once per {@code withContext} operator.
     *
     * @return the context, must not be {@code null}.
     */
    default Context context() {
        return Context.empty();
    }
}
