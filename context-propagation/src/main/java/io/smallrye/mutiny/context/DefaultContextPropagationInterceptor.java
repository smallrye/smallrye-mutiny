package io.smallrye.mutiny.context;

import io.smallrye.context.SmallRyeThreadContext;

/**
 * Provides context propagation by intercepting the user callbacks.
 */
public class DefaultContextPropagationInterceptor extends BaseContextPropagationInterceptor {

    @Override
    protected SmallRyeThreadContext getThreadContext() {
        return SmallRyeThreadContext.getCurrentThreadContextOrDefaultContexts();
    }
}
