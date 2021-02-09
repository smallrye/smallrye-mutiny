package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.DONE;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Executor;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniRunSubscribeOn<I> extends UniOperator<I, I> {

    private final Executor executor;

    public UniRunSubscribeOn(Uni<? extends I> upstream, Executor executor) {
        super(nonNull(upstream, "upstream"));
        this.executor = nonNull(executor, "executor");
    }

    @Override
    public void subscribe(UniSubscriber<? super I> subscriber) {
        try {
            executor.execute(() -> {
                try {
                    AbstractUni.subscribe(upstream(), new UniRunSubscribeOnProcessor(subscriber));
                } catch (Throwable woops) {
                    forwardFailure(subscriber, woops);
                }
            });
        } catch (Throwable err) {
            forwardFailure(subscriber, err);
        }
    }

    private void forwardFailure(UniSubscriber<? super I> subscriber, Throwable failure) {
        subscriber.onSubscribe(DONE);
        subscriber.onFailure(failure);
    }

    // Note we plug a UniOperatorProcessor for ensuring an unwrapped subscription.
    // This may be revisited in future iterations.
    private class UniRunSubscribeOnProcessor extends UniOperatorProcessor<I, I> {
        public UniRunSubscribeOnProcessor(UniSubscriber<? super I> downstream) {
            super(downstream);
        }
    }
}
