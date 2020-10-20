package io.smallrye.mutiny.context;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;

/**
 * Provides context propagation to Multi types.
 */
public class DefaultContextPropagationMultiInterceptor extends ContextPropagationMultiInterceptor {

    static final ThreadContext THREAD_CONTEXT = ContextManagerProvider.instance().getContextManager()
            .newThreadContextBuilder().build();

    @Override
    protected ThreadContext getThreadContext() {
        return THREAD_CONTEXT;
    }
}
