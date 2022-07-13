package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiLogger<T> extends AbstractMultiOperator<T, T> {

    private final String identifier;
    private final AtomicLong increment = new AtomicLong(0L);

    public MultiLogger(Multi<? extends T> upstream, String identifier) {
        super(nonNull(upstream, "upstream"));
        String id = nonNull(identifier, "identifier");
        if (id.isEmpty()) {
            throw new IllegalArgumentException("The identifier cannot be an empty string");
        }
        this.identifier = id;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        upstream.subscribe().withSubscriber(new MultiLoggerProcessor(subscriber, increment.getAndIncrement()));
    }

    private class MultiLoggerProcessor extends MultiOperatorProcessor<T, T> {

        private final String processorIdentifier;

        public MultiLoggerProcessor(MultiSubscriber<? super T> downstream, long increment) {
            super(downstream);
            this.processorIdentifier = identifier + "." + increment;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (!isDone()) {
                Infrastructure.logFromOperator(processorIdentifier, "onSubscribe", null, null);
                super.onSubscribe(subscription);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (!isDone()) {
                Infrastructure.logFromOperator(processorIdentifier, "onFailure", null, failure);
                super.onFailure(failure);
            }
        }

        @Override
        public void onItem(T item) {
            if (!isDone()) {
                Infrastructure.logFromOperator(processorIdentifier, "onItem", item, null);
                super.onItem(item);
            }
        }

        @Override
        public void onCompletion() {
            if (!isDone()) {
                Infrastructure.logFromOperator(processorIdentifier, "onCompletion", null, null);
                super.onCompletion();
            }
        }

        @Override
        public void request(long numberOfItems) {
            if (!isDone()) {
                Infrastructure.logFromOperator(processorIdentifier, "request", numberOfItems, null);
                super.request(numberOfItems);
            }
        }

        @Override
        public void cancel() {
            if (!isDone()) {
                Infrastructure.logFromOperator(processorIdentifier, "cancel", null, null);
                super.cancel();
            }
        }
    }
}
