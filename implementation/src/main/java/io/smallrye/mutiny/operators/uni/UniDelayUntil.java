package io.smallrye.mutiny.operators.uni;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniDelayUntil<T> extends UniOperator<T, T> {
    private final Function<? super T, Uni<?>> function;
    private final ScheduledExecutorService executor;

    public UniDelayUntil(Uni<T> upstream, Function<? super T, Uni<?>> function,
            ScheduledExecutorService executor) {
        super(upstream);
        this.function = function;
        this.executor = executor;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniDelayUntilProcessor(subscriber));
    }

    private class UniDelayUntilProcessor extends UniOperatorProcessor<T, T> {

        public UniDelayUntilProcessor(UniSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onItem(T item) {
            if (!isCancelled()) {
                try {
                    Uni<?> uni = function.apply(item);
                    if (uni == null) {
                        super.onFailure(new NullPointerException("The function returned `null` instead of a valid `Uni`"));
                        return;
                    }
                    uni.runSubscriptionOn(executor).subscribe().with(ignored -> super.onItem(item), super::onFailure);
                } catch (Throwable err) {
                    super.onFailure(err);
                }
            }
        }
    }
}
