package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniLogger<T> extends UniOperator<T, T> {

    private final String identifier;
    private final AtomicLong increment = new AtomicLong(0L);

    public UniLogger(Uni<? extends T> upstream, String identifier) {
        super(nonNull(upstream, "upstream"));
        String id = nonNull(identifier, "identifier");
        if (id.isEmpty()) {
            throw new IllegalArgumentException("The identifier cannot be an empty string");
        }
        this.identifier = id;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        AbstractUni.subscribe(upstream(), new UniLoggerProcessor(subscriber, increment.getAndIncrement()));
    }

    private class UniLoggerProcessor extends UniOperatorProcessor<T, T> {

        private final String processorIdentifier;

        public UniLoggerProcessor(UniSubscriber<? super T> downstream, long increment) {
            super(downstream);
            this.processorIdentifier = identifier + "." + increment;
        }

        @Override
        public void onSubscribe(UniSubscription subscription) {
            if (!isCancelled()) {
                Infrastructure.logFromOperator(processorIdentifier, "onSubscribe", null, null);
                super.onSubscribe(subscription);
            }
        }

        @Override
        public void onItem(T item) {
            if (!isCancelled()) {
                Infrastructure.logFromOperator(processorIdentifier, "onItem", item, null);
                super.onItem(item);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (!isCancelled()) {
                Infrastructure.logFromOperator(processorIdentifier, "onFailure", null, failure);
                super.onFailure(failure);
            }
        }

        @Override
        public void cancel() {
            if (!isCancelled()) {
                Infrastructure.logFromOperator(processorIdentifier, "cancel", null, null);
                super.cancel();
            }
        }
    }
}
