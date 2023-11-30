package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.Flow.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Concatenates a fixed set of Publishers.
 * Items from each publisher are emitted in order.
 * All the items from one publisher must be consumed before items from another publisher are emitted.
 *
 * @param <T> the type of item
 */
public class MultiConcatOp<T> extends AbstractMulti<T> {

    private final Publisher<? extends T>[] publishers;

    private final boolean postponeFailurePropagation;

    @SafeVarargs
    public MultiConcatOp(boolean postponeFailurePropagation, Publisher<? extends T>... publishers) {
        this.publishers = ParameterValidation.doesNotContainNull(publishers, "publishers");
        this.postponeFailurePropagation = postponeFailurePropagation;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        if (actual == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        if (publishers.length == 0) {
            Subscriptions.complete(actual);
            return;
        }

        if (publishers.length == 1) {
            publishers[0].subscribe(Infrastructure.onMultiSubscription(publishers[0], actual));
            return;
        }

        if (postponeFailurePropagation) {
            Multi.createFrom().items(publishers)
                    .onItem().transformToMulti(publisher -> publisher)
                    .collectFailures()
                    .concatenate()
                    .subscribe().withSubscriber(actual);
        } else {
            Multi.createFrom().items(publishers)
                    .onItem().transformToMulti(publisher -> publisher)
                    .concatenate()
                    .subscribe().withSubscriber(actual);
        }
    }
}
