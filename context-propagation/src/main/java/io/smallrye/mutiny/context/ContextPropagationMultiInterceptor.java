package io.smallrye.mutiny.context;

import java.util.Objects;
import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.StrictMultiSubscriber;
import io.smallrye.mutiny.infrastructure.MultiInterceptor;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Provides context propagation to Multi types.
 * Subclasses need to override this to provide the Context Propagation ThreadContext.
 */
public abstract class ContextPropagationMultiInterceptor implements MultiInterceptor {

    @Override
    public <T> Subscriber<? super T> onSubscription(Publisher<? extends T> instance, Subscriber<? super T> subscriber) {
        Executor executor = getThreadContext().currentContextExecutor();
        return new ContextPropagationSubscriber<>(executor, subscriber);
    }

    @Override
    public <T> Multi<T> onMultiCreation(Multi<T> multi) {
        Executor executor = getThreadContext().currentContextExecutor();
        return new ContextPropagationMulti<>(executor, multi);
    }

    /**
     * Gets the Context Propagation ThreadContext. External
     * implementations may implement this method.
     *
     * @return the ThreadContext
     * @see DefaultContextPropagationMultiInterceptor#getThreadContext()
     */
    protected abstract ThreadContext getThreadContext();

    private static class ContextPropagationMulti<T> extends AbstractMulti<T> {

        private final Executor executor;
        private final Multi<T> multi;

        public ContextPropagationMulti(Executor executor, Multi<T> multi) {
            this.executor = executor;
            this.multi = multi;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            Objects.requireNonNull(subscriber); // Required by reactive streams TCK
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (subscriber instanceof MultiSubscriber) {
                        multi.subscribe(subscriber);
                    } else {
                        multi.subscribe(new StrictMultiSubscriber<>(subscriber));
                    }
                }
            });
        }
    }

    @SuppressWarnings({ "ReactiveStreamsSubscriberImplementation" })
    public static class ContextPropagationSubscriber<T> implements Subscriber<T> {

        private final Executor executor;
        private final Subscriber<? super T> subscriber;

        public ContextPropagationSubscriber(Executor executor, Subscriber<? super T> subscriber) {
            this.executor = executor;
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            executor.execute(() -> subscriber.onSubscribe(subscription));
        }

        @Override
        public void onNext(T item) {
            Objects.requireNonNull(item);
            executor.execute(() -> subscriber.onNext(item));
        }

        @Override
        public void onError(Throwable failure) {
            Objects.requireNonNull(failure);
            executor.execute(() -> subscriber.onError(failure));
        }

        @Override
        public void onComplete() {
            executor.execute(subscriber::onComplete);
        }
    }
}
