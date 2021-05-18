package mutiny.zero;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public interface PublisherHelpers {

    /**
     * Collect all items as a list.
     * 
     * @param publisher the publisher
     * @param <T> the emitted type
     * @return the future accumulating the items into a list.
     */
    static <T> CompletionStage<List<T>> collectToList(Publisher<T> publisher) {
        List<T> list = new ArrayList<>();
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T t) {
                list.add(t);
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                future.complete(list);
            }
        });
        return future;
    }

    /**
     * Simple map implementation.
     * 
     * @param source the upstream
     * @param mapper the mapper
     * @param <I> the input type
     * @param <O> the output type
     * @return the mapped publisher.
     */
    static <I, O> Publisher<O> map(Publisher<I> source, Function<I, O> mapper) {
        return new Processor<I, O>() {

            private Subscription upstream;
            private Subscriber<? super O> downstream;

            @Override
            public void onSubscribe(Subscription subscription) {
                upstream = subscription;
                downstream.onSubscribe(subscription);
            }

            @Override
            public void onNext(I i) {
                O res;
                try {
                    res = Objects.requireNonNull(mapper.apply(i));
                } catch (Exception e) {
                    upstream.cancel();
                    onError(e);
                    return;
                }
                downstream.onNext(res);
            }

            @Override
            public void onError(Throwable throwable) {
                downstream.onError(throwable);
            }

            @Override
            public void onComplete() {
                downstream.onComplete();
            }

            @Override
            public void subscribe(Subscriber<? super O> subscriber) {
                this.downstream = subscriber;
                source.subscribe(this);
            }
        };
    }
}
