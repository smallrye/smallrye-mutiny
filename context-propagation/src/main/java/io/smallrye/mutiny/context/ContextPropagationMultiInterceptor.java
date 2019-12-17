package io.smallrye.mutiny.context;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.MultiInterceptor;
import io.smallrye.mutiny.operators.AbstractMulti;

/**
 * Provides context propagation to Multi types.
 */
public class ContextPropagationMultiInterceptor implements MultiInterceptor {

    static final ThreadContext THREAD_CONTEXT = ContextManagerProvider.instance().getContextManager()
            .newThreadContextBuilder().build();

    @SuppressWarnings("SubscriberImplementation")
    @Override
    public <T> Subscriber<? super T> onSubscription(Publisher<? extends T> instance, Subscriber<? super T> subscriber) {
        Executor executor = THREAD_CONTEXT.currentContextExecutor();
        return new Subscriber<T>() {

            @Override
            public void onSubscribe(Subscription subscription) {
                executor.execute(() -> subscriber.onSubscribe(subscription));
            }

            @Override
            public void onNext(T item) {
                executor.execute(() -> subscriber.onNext(item));
            }

            @Override
            public void onError(Throwable failure) {
                executor.execute(() -> subscriber.onError(failure));
            }

            @Override
            public void onComplete() {
                executor.execute(subscriber::onComplete);
            }
        };
    }

    @Override
    public <T> Multi<T> onMultiCreation(Multi<T> multi) {
        Executor executor = THREAD_CONTEXT.currentContextExecutor();
        return new AbstractMulti<T>() {
            @Override
            protected Publisher<T> publisher() {
                return this;
            }

            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                executor.execute(() -> multi.subscribe().withSubscriber(subscriber));
            }
        };
    }
}
