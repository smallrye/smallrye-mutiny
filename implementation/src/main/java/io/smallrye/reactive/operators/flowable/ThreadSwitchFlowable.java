package io.smallrye.reactive.operators.flowable;

import java.util.concurrent.Executor;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.Flowable;

public class ThreadSwitchFlowable<T> extends Flowable<T> {

    private final Publisher<T> upstream;
    private final Executor onItemExecutor;
    private final Executor onFailureExecutor;
    private final Executor onCompletionExecutor;

    public ThreadSwitchFlowable(Publisher<T> upstream, Executor onItemExecutor, Executor onFailureExecutor,
            Executor onCompletionExecutor) {
        this.upstream = upstream;

        this.onItemExecutor = onItemExecutor;
        this.onFailureExecutor = onFailureExecutor;
        this.onCompletionExecutor = onCompletionExecutor;

    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        @SuppressWarnings("SubscriberImplementation")
        Subscriber<? super T> delegate = new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                s.onSubscribe(subscription);
            }

            @Override
            public void onNext(T t) {
                if (onItemExecutor != null) {
                    onItemExecutor.execute(() -> s.onNext(t));
                } else {
                    s.onNext(t);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (onFailureExecutor != null) {
                    onFailureExecutor.execute(() -> s.onError(throwable));
                } else {
                    s.onError(throwable);
                }
            }

            @Override
            public void onComplete() {
                if (onCompletionExecutor != null) {
                    onCompletionExecutor.execute(s::onComplete);
                } else {
                    s.onComplete();
                }
            }
        };
        upstream.subscribe(delegate);
    }
}
