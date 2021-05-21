package mutiny.zero.internal;

import java.util.Objects;
import java.util.function.Function;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class MapOperator<I, O> implements Publisher<O> {

    private final Publisher<I> upstream;
    private final Function<I, O> mapper;

    public MapOperator(Publisher<I> upstream, Function<I, O> mapper) {
        this.upstream = upstream;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Subscriber<? super O> subscriber) {
        upstream.subscribe(new MapProcessor(subscriber));
    }

    private class MapProcessor implements Processor<I, O>, Subscription {

        private final Subscriber<? super O> downstream;
        private Subscription subscription;
        private volatile boolean cancelled = false;

        public MapProcessor(Subscriber<? super O> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void subscribe(Subscriber<? super O> subscriber) {
            System.out.println("??? " + subscriber);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            downstream.onSubscribe(this);
        }

        @Override
        public void onNext(I i) {
            if (cancelled) {
                return;
            }
            O res;
            try {
                res = Objects.requireNonNull(mapper.apply(i));
            } catch (Exception e) {
                subscription.cancel();
                onError(e);
                return;
            }
            downstream.onNext(res);
        }

        @Override
        public void onError(Throwable throwable) {
            if (cancelled) {
                return;
            }
            cancelled = true;
            downstream.onError(throwable);
        }

        @Override
        public void onComplete() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            downstream.onComplete();
        }

        @Override
        public void request(long n) {
            if (cancelled) {
                return;
            }
            subscription.request(n);
        }

        @Override
        public void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            subscription.cancel();
        }
    }
}
