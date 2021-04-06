package mutiny.zero.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class PublisherToCompletionStageSubscriber<T> implements Subscriber<T> {

    private final CompletableFuture<T> future;
    private final AtomicBoolean completed = new AtomicBoolean();
    private Subscription subscription;

    public PublisherToCompletionStageSubscriber(CompletableFuture<T> future) {
        this.future = future;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1L);
    }

    @Override
    public void onNext(T value) {
        if (completed.compareAndSet(false, true)) {
            subscription.cancel();
            future.complete(value);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (completed.compareAndSet(false, true)) {
            subscription.cancel();
            future.completeExceptionally(throwable);
        }
    }

    @Override
    public void onComplete() {
        // Ignore
    }
}
