package io.smallrye.mutiny.context;

import org.eclipse.microprofile.context.spi.ContextManagerProvider;

import io.smallrye.context.SmallRyeThreadContext;

/**
 * Provides context propagation by intercepting the user callbacks.
 */
public class DefaultContextPropagationInterceptor extends BaseContextPropagationInterceptor {

    static final SmallRyeThreadContext THREAD_CONTEXT = (SmallRyeThreadContext) ContextManagerProvider.instance()
            .getContextManager()
            .newThreadContextBuilder().build();

    @Override
    protected SmallRyeThreadContext getThreadContext() {
        return THREAD_CONTEXT;
    }
}
