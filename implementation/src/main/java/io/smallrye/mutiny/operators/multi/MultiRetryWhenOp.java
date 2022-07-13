package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.subscription.ContextSupport;
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

    private final Function<? super Multi<Throwable>, ? extends Publisher<?>> triggerStreamFactory;
    private final Predicate<? super Throwable> onFailurePredicate;

    public MultiRetryWhenOp(Multi<? extends T> upstream, Predicate<? super Throwable> onFailurePredicate,
            Function<? super Multi<Throwable>, ? extends Publisher<?>> triggerStreamFactory) {
        super(upstream);
        this.onFailurePredicate = onFailurePredicate;
        this.triggerStreamFactory = triggerStreamFactory;
    }

    private static <T> void subscribe(MultiSubscriber<? super T> downstream, Predicate<? super Throwable> onFailurePredicate,
            Function<? super Multi<Throwable>, ? extends Publisher<?>> triggerStreamFactory,
            Multi<? extends T> upstream) {
        TriggerSubscriber other = new TriggerSubscriber();
        Subscriber<Throwable> signaller = new SerializedSubscriber<>(other.processor);
        signaller.onSubscribe(Subscriptions.empty());
        MultiSubscriber<T> serialized = new SerializedSubscriber<>(downstream);

        RetryWhenOperator<T> operator = new RetryWhenOperator<>(upstream, onFailurePredicate, serialized, signaller);
        other.operator = operator;

        serialized.onSubscribe(operator);
        Publisher<?> publisher;

        try {
            publisher = triggerStreamFactory.apply(other);
            if (publisher == null) {
                throw new NullPointerException("The stream factory returned `null`");
            }
        } catch (Throwable e) {
            downstream.onFailure(e);
            return;
        }

        publisher.subscribe(other);

        if (!operator.isCancelled()) {
            upstream.subscribe(operator);
        }
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        subscribe(downstream, onFailurePredicate, triggerStreamFactory, upstream);
    }

    static final class RetryWhenOperator<T> extends SwitchableSubscriptionSubscriber<T> {

        private final Publisher<? extends T> upstream;
        private final AtomicInteger wip = new AtomicInteger();
        private final Subscriber<Throwable> signaller;
        private final Subscriptions.DeferredSubscription arbiter = new Subscriptions.DeferredSubscription();
        private final Predicate<? super Throwable> onFailurePredicate;

        long produced;

        RetryWhenOperator(Publisher<? extends T> upstream, Predicate<? super Throwable> onFailurePredicate,
                MultiSubscriber<? super T> downstream,
                Subscriber<Throwable> signaller) {
            super(downstream);
            this.onFailurePredicate = onFailurePredicate;
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

        public void setWhen(Flow.Subscription w) {
            arbiter.set(w);
        }

        @Override
        public void onItem(T t) {
            downstream.onItem(t);
            produced++;
        }

        @Override
        public void onFailure(Throwable t) {
            if (testOnFailurePredicate(t)) {
                return;
            }

            long p = produced;
            if (p != 0L) {
                produced = 0;
                emitted(p);
            }
            arbiter.request(1);
            signaller.onNext(t);
        }

        private boolean testOnFailurePredicate(Throwable t) {
            try {
                if (!onFailurePredicate.test(t)) {
                    arbiter.cancel();
                    downstream.onFailure(t);
                }
            } catch (Throwable e) {
                arbiter.cancel();
                downstream.onFailure(new CompositeException(e, t));
                return true;
            }
            return false;
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
    static final class TriggerSubscriber extends AbstractMulti<Throwable>
            implements Multi<Throwable>, Subscriber<Object>, ContextSupport {
        RetryWhenOperator<?> operator;
        private final Flow.Processor<Throwable, Throwable> processor = UnicastProcessor.<Throwable> create().serialized();
        private Context context;

        @Override
        public void onSubscribe(Flow.Subscription s) {
            operator.setWhen(s);
        }

        @Override
        public void onNext(Object t) {
            operator.resubscribe();
        }

        @Override
        public void onError(Throwable t) {
            operator.whenFailure(t);
        }

        @Override
        public void onComplete() {
            operator.whenComplete();
        }

        @Override
        public void subscribe(Subscriber<? super Throwable> actual) {
            if (actual instanceof ContextSupport) {
                this.context = ((ContextSupport) actual).context();
            } else {
                this.context = Context.empty();
            }
            processor.subscribe(actual);
        }

        @Override
        public Context context() {
            return this.context;
        }
    }

}
