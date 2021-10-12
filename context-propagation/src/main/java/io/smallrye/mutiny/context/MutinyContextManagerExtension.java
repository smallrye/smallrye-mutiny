package io.smallrye.mutiny.context;

import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;

import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class MutinyContextManagerExtension implements ContextManagerExtension {

    @Override
    public void setup(ContextManager manager) {
        Infrastructure.setCompletableFutureWrapper(new UnaryOperator<CompletableFuture<?>>() {
            @Override
            public CompletableFuture<?> apply(CompletableFuture<?> t) {
                ThreadContext threadContext = SmallRyeThreadContext.getCurrentThreadContextOrDefaultContexts();
                return threadContext.withContextCapture(t);
            }
        });
    }

}
