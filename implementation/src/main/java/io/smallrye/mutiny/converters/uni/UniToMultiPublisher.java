package io.smallrye.mutiny.converters.uni;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public final class UniToMultiPublisher<T> implements Flow.Publisher<T> {

    private final Uni<T> uni;

    public UniToMultiPublisher(Uni<T> uni) {
        this.uni = uni;
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        downstream.onSubscribe(new UniToMultiSubscription<>(uni, downstream));
    }

    private static class UniToMultiSubscription<T> implements UniSubscription, Subscription, UniSubscriber<T>, ContextSupport {

        private final Uni<T> uni;
        private final Subscriber<? super T> downstream;

        enum State {
            INIT,
            UNI_REQUESTED,
            DONE
        }

        private volatile UniSubscription upstream;
        private volatile State state = State.INIT;

        private static final AtomicReferenceFieldUpdater<UniToMultiSubscription, State> STATE_UPDATER = AtomicReferenceFieldUpdater
                .newUpdater(UniToMultiSubscription.class, State.class, "state");

        private UniToMultiSubscription(Uni<T> uni, Subscriber<? super T> downstream) {
            this.uni = uni;
            this.downstream = downstream;
        }

        @Override
        public Context context() {
            if (downstream instanceof ContextSupport) {
                return ((ContextSupport) downstream).context();
            } else {
                return Context.empty();
            }
        }

        @Override
        public void cancel() {
            if (upstream != null) {
                upstream.cancel();
            }
        }

        @Override
        public void request(long n) {
            if (n <= 0L) {
                downstream.onError(new IllegalArgumentException("Invalid request"));
                return;
            }
            if (STATE_UPDATER.compareAndSet(this, State.INIT, State.UNI_REQUESTED)) {
                AbstractUni.subscribe(uni, this);
            }
        }

        @Override
        public void onSubscribe(UniSubscription subscription) {
            if (upstream == null) {
                upstream = subscription;
            } else {
                subscription.cancel();
                downstream.onError(new IllegalStateException(
                        "Invalid subscription state - already have a subscription for upstream"));
            }
        }

        @Override
        public void onItem(T item) {
            if (STATE_UPDATER.compareAndSet(this, State.UNI_REQUESTED, State.DONE)) {
                if (item != null) {
                    downstream.onNext(item);
                }
                downstream.onComplete();
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (STATE_UPDATER.compareAndSet(this, UniToMultiSubscription.State.UNI_REQUESTED,
                    UniToMultiSubscription.State.DONE)) {
                downstream.onError(failure);
            }
        }
    }
}
