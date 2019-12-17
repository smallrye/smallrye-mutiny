package io.smallrye.mutiny.context;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;

import io.smallrye.mutiny.infrastructure.Infrastructure;

public class MutinyContextManagerExtension implements ContextManagerExtension {

    @Override
    public void setup(ContextManager manager) {
        ThreadContext threadContext = manager.newThreadContextBuilder().build();
        Infrastructure.setCompletableFutureWrapper(threadContext::withContextCapture);
    }

}
