package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.MAPPER_RETURNED_NULL;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnItemTransformToMulti<I, O> extends AbstractMulti<O> {

    private final Function<? super I, ? extends Flow.Publisher<? extends O>> mapper;
    private final Uni<I> upstream;

    public UniOnItemTransformToMulti(Uni<I> upstream, Function<? super I, ? extends Flow.Publisher<? extends O>> mapper) {
        this.upstream = upstream;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(MultiSubscriber<? super O> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        AbstractUni.subscribe(upstream, new FlatMapPublisherSubscriber<>(subscriber, mapper));
    }

    @SuppressWarnings("SubscriberImplementation")
    static final class FlatMapPublisherSubscriber<I, O>
            implements Subscriber<O>, UniSubscriber<I>, Flow.Subscription, ContextSupport {

        private final AtomicReference<Flow.Subscription> secondUpstream;
        private final AtomicReference<UniSubscription> firstUpstream;
        private final Subscriber<? super O> downstream;
        private final Function<? super I, ? extends Flow.Publisher<? extends O>> mapper;
        private final AtomicLong requested = new AtomicLong();

        FlatMapPublisherSubscriber(Subscriber<? super O> downstream,
                Function<? super I, ? extends Flow.Publisher<? extends O>> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.firstUpstream = new AtomicReference<>();
            this.secondUpstream = new AtomicReference<>();
        }

        @Override
        public void onNext(O item) {
            downstream.onNext(item);
        }

        @Override
        public void onError(Throwable failure) {
            downstream.onError(failure);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void request(long n) {
            Subscriptions.requestIfNotNullOrAccumulate(secondUpstream, requested, n);
        }

        @Override
        public void cancel() {
            UniSubscription subscription = firstUpstream.getAndSet(EmptyUniSubscription.CANCELLED);
            if (subscription != null && subscription != EmptyUniSubscription.CANCELLED) {
                subscription.cancel();
            }
            Subscriptions.cancel(secondUpstream);
        }

        @Override
        public Context context() {
            if (downstream instanceof ContextSupport) {
                return ((ContextSupport) downstream).context();
            } else {
                return Context.empty();
            }
        }

        /**
         * Called when we get the subscription from the upstream UNI
         *
         * @param subscription the subscription allowing to cancel the computation.
         */
        @Override
        public void onSubscribe(UniSubscription subscription) {
            if (firstUpstream.compareAndSet(null, subscription)) {
                downstream.onSubscribe(this);
            }
        }

        /**
         * Called after we produced the {@link Flow.Publisher} and subscribe on it.
         *
         * @param subscription the subscription from the produced {@link Flow.Publisher}
         */
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (secondUpstream.compareAndSet(null, subscription)) {
                long r = requested.getAndSet(0L);
                if (r != 0L) {
                    subscription.request(r);
                }
            }
        }

        @Override
        public void onItem(I item) {
            Flow.Publisher<? extends O> publisher;

            try {
                publisher = mapper.apply(item);
                if (publisher == null) {
                    throw new NullPointerException(MAPPER_RETURNED_NULL);
                }
            } catch (Throwable ex) {
                downstream.onError(ex);
                return;
            }

            publisher.subscribe(this);
        }

        @Override
        public void onFailure(Throwable failure) {
            downstream.onError(failure);
        }
    }
}
