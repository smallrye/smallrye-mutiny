package io.smallrye.mutiny.operators.uni;

import java.util.function.Consumer;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnSubscribeInvoke<T> extends UniOperator<T, T> {

    private final Consumer<? super UniSubscription> callback;

    public UniOnSubscribeInvoke(Uni<? extends T> upstream,
            Consumer<? super UniSubscription> callback) {
        super(upstream);
        this.callback = callback;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniOnSubscribeInvokeProcessor(subscriber));
    }

    private class UniOnSubscribeInvokeProcessor extends UniOperatorProcessor<T, T> {

        public UniOnSubscribeInvokeProcessor(UniSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(UniSubscription subscription) {
            // Invoke callback
            try {
                callback.accept(subscription);
            } catch (Throwable e) {
                subscription.cancel();
                downstream.onSubscribe(EmptyUniSubscription.DONE);
                downstream.onFailure(e);
                return;
            }

            // Pass the subscription downstream
            // Cannot be done in the try block as it may propagates 2 subscriptions.
            super.onSubscribe(subscription);
        }
    }
}
