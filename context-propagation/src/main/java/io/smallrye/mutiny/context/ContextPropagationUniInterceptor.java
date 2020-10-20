package io.smallrye.mutiny.context;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.UniInterceptor;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniSerializedSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * Provides context propagation to Uni types.
 */
public class ContextPropagationUniInterceptor implements UniInterceptor {

    @Override
    public <T> UniSubscriber<? super T> onSubscription(Uni<T> instance, UniSubscriber<? super T> subscriber) {
        Executor executor = threadContext().currentContextExecutor();
        return new UniSubscriber<T>() {

            @Override
            public void onSubscribe(UniSubscription subscription) {
                executor.execute(() -> subscriber.onSubscribe(subscription));
            }

            @Override
            public void onItem(T item) {
                executor.execute(() -> subscriber.onItem(item));
            }

            @Override
            public void onFailure(Throwable failure) {
                executor.execute(() -> subscriber.onFailure(failure));
            }
        };
    }

    @Override
    public <T> Uni<T> onUniCreation(Uni<T> uni) {
        Executor executor = threadContext().currentContextExecutor();
        return new AbstractUni<T>() {
            @Override
            protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
                executor.execute(() -> AbstractUni.subscribe(uni, subscriber));
            }
        };
    }

    private static ThreadContext threadContext() {
        return ContextManagerProvider.instance().getContextManager()
                .newThreadContextBuilder().build();
    }

}
