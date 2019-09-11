package io.smallrye.reactive.context;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.infrastructure.UniInterceptor;
import io.smallrye.reactive.operators.AbstractUni;
import io.smallrye.reactive.operators.UniSerializedSubscriber;
import io.smallrye.reactive.subscription.UniSubscriber;
import io.smallrye.reactive.subscription.UniSubscription;

/**
 * Provides context propagation to Uni types.
 */
public class ContextPropagationUniInterceptor implements UniInterceptor{
    
    ThreadContext TC = ContextManagerProvider.instance().getContextManager().newThreadContextBuilder().build();
    
    @Override
    public <T> UniSubscriber<? super T> onSubscription(Uni<T> instance, UniSubscriber<? super T> subscriber) {
        Executor executor = TC.currentContextExecutor();
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
        Executor executor = TC.currentContextExecutor();
        return new AbstractUni<T>() {
            @Override
            protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
                executor.execute(() -> uni.subscribe().withSubscriber(subscriber));
            }
        };
    }
}
