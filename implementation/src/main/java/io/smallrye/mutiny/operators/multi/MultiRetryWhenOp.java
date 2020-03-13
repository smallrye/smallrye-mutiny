package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.operators.multi.processors.DirectProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.SerializedSubscriber;
import io.smallrye.mutiny.subscription.SwitchableSubscriptionSubscriber;

/**
 * Retries a source when a companion stream signals an item in response to the main's failure event.
 * <p>
 * If the companion stream signals when the main source is active, the repeat
 * attempt is suppressed and any terminal signal will terminate the main source with the same signal immediately.
 *
 * @param <T> the type of item
 */
public final class MultiRetryWhenOp<T> extends AbstractMultiOperator<T, T> {

    final Function<? super Multi<Throwable>, ? extends Publisher<?>> whenStreamFactory;

    public MultiRetryWhenOp(Multi<? extends T> upstream,
            Function<? super Multi<Throwable>, ? extends Publisher<?>> whenStreamFactory) {
        super(upstream);
        this.whenStreamFactory = ParameterValidation.nonNull(whenStreamFactory, "whenStreamFactory");
    }

    static <T> void subscribe(MultiSubscriber<? super T> downstream,
            Function<? super Multi<Throwable>, ? extends Publisher<?>> whenStreamFactory,
            Multi<? extends T> upstream) {
        RetryWhenOtherSubscriber other = new RetryWhenOtherSubscriber();
        Subscriber<Throwable> signaller = new SerializedSubscriber<>(other.processor);
        signaller.onSubscribe(Subscriptions.empty());
        MultiSubscriber<T> serialized = new SerializedSubscriber<>(downstream);

        RetryWhenMainSubscriber<T> main = new RetryWhenMainSubscriber<>(upstream, serialized, signaller);
        other.main = main;

        serialized.onSubscribe(main);
        Publisher<?> publisher;

        try {
            publisher = whenStreamFactory.apply(other);
            if (publisher == null) {
                throw new NullPointerException("The stream factory returned `null`");
            }
        } catch (Throwable e) {
            downstream.onError(e);
            return;
        }

        publisher.subscribe(other);

        if (!main.isCancelled()) {
            upstream.subscribe(main);
        }
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        subscribe(downstream, whenStreamFactory, upstream);
    }

    static final class RetryWhenMainSubscriber<T> extends SwitchableSubscriptionSubscriber<T> {

        private final Publisher<? extends T> upstream;
        private final AtomicInteger wip = new AtomicInteger();
        private final Subscriber<Throwable> signaller;
        private final Subscriptions.DeferredSubscription arbiter = new Subscriptions.DeferredSubscription();

        long produced;

        RetryWhenMainSubscriber(Publisher<? extends T> upstream, MultiSubscriber<? super T> downstream,
                Subscriber<Throwable> signaller) {
            super(downstream);
            this.upstream = upstream;
            this.signaller = signaller;
        }

        @Override
        public void cancel() {
            if (!isCancelled()) {
                arbiter.cancel();
                super.cancel();
            }

        }

        public void setWhen(Subscription w) {
            arbiter.set(w);
        }

        @Override
        public void onItem(T t) {
            downstream.onItem(t);
            produced++;
        }

        @Override
        public void onFailure(Throwable t) {
            long p = produced;
            if (p != 0L) {
                produced = 0;
                emitted(p);
            }
            arbiter.request(1);
            signaller.onNext(t);
        }

        @Override
        public void onCompletion() {
            arbiter.cancel();
            downstream.onComplete();
        }

        void resubscribe() {
            if (wip.getAndIncrement() == 0) {
                do {
                    if (isCancelled()) {
                        return;
                    }

                    upstream.subscribe(this);

                } while (wip.decrementAndGet() != 0);
            }
        }

        void whenFailure(Throwable failure) {
            super.cancel();
            downstream.onFailure(failure);
        }

        void whenComplete() {
            super.cancel();
            downstream.onComplete();
        }
    }

    @SuppressWarnings({ "SubscriberImplementation" })
    static final class RetryWhenOtherSubscriber extends AbstractMulti<Throwable>
            implements Multi<Throwable>, Subscriber<Object> {
        RetryWhenMainSubscriber<?> main;
        final DirectProcessor<Throwable> processor = new DirectProcessor<>();

        @Override
        public void onSubscribe(Subscription s) {
            main.setWhen(s);
        }

        @Override
        public void onNext(Object t) {
            main.resubscribe();
        }

        @Override
        public void onError(Throwable t) {
            main.whenFailure(t);
        }

        @Override
        public void onComplete() {
            main.whenComplete();
        }

        @Override
        public void subscribe(Subscriber<? super Throwable> actual) {
            processor.subscribe(actual);
        }
    }

}
