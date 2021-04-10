package mutiny.zero.internal;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class CompletionStagePublisher<T> implements Publisher<T> {

    private final Supplier<CompletionStage<T>> completionStageSupplier;

    public CompletionStagePublisher(Supplier<CompletionStage<T>> completionStageSupplier) {
        this.completionStageSupplier = completionStageSupplier;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        CompletionStage<T> cs = completionStageSupplier.get();
        if (cs == null) {
            subscriber.onSubscribe(new AlreadyCompletedSubscription());
            subscriber.onError(new NullPointerException("The completion stage is null"));
            return;
        }
        CompletableFuture<T> completableFuture = cs.toCompletableFuture();
        if (completableFuture.isDone()) {
            subscriber.onSubscribe(new AlreadyCompletedSubscription());
            try {
                T value = completableFuture.get();
                if (value == null) {
                    subscriber.onError(new NullPointerException("The CompletionStage produced a null value"));
                } else {
                    subscriber.onNext(value);
                    subscriber.onComplete();
                }
            } catch (InterruptedException e) {
                subscriber.onError(e);
            } catch (ExecutionException e) {
                subscriber.onError(e.getCause());
            }
        } else {
            subscriber.onSubscribe(new CompletionStageSubscription(subscriber, completableFuture));
        }
    }

    private class CompletionStageSubscription implements Subscription {

        private final Subscriber<? super T> subscriber;
        private final CompletableFuture<T> completableFuture;
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private CompletionStageSubscription(Subscriber<? super T> subscriber, CompletableFuture<T> completableFuture) {
            this.subscriber = subscriber;
            this.completableFuture = completableFuture;
        }

        @Override
        public void request(long n) {
            if (cancelled.get()) {
                return;
            }
            if (n <= 0L) {
                cancel();
                subscriber.onError(Helper.negativeRequest(n));
            } else {
                completableFuture.whenComplete((value, err) -> {
                    if (cancelled.compareAndSet(false, true)) {
                        if (err != null) {
                            subscriber.onError(err);
                        } else if (value == null) {
                            subscriber.onError(new NullPointerException("The CompletionStage produced a null value"));
                        } else {
                            subscriber.onNext(value);
                            subscriber.onComplete();
                        }
                    }
                });
            }
        }

        @Override
        public void cancel() {
            cancelled.set(false);
            completableFuture.toCompletableFuture().cancel(false);
        }
    }
}
