package io.smallrye.mutiny.context;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;

/**
 * Provides context propagation to Uni types.
 */
public class DefaultContextPropagationUniInterceptor extends ContextPropagationUniInterceptor {

    static final ThreadContext THREAD_CONTEXT = ContextManagerProvider.instance().getContextManager()
            .newThreadContextBuilder().build();

    @Override
    protected ThreadContext getThreadContext() {
        return THREAD_CONTEXT;
    }
}
