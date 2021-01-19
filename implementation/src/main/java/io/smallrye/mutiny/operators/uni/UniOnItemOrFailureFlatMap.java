package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.operators.uni.UniOnItemTransformToUni.handleInnerSubscription;

import java.util.function.BiFunction;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniDelegatingSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnItemOrFailureFlatMap<I, O> extends UniOperator<I, O> {

    private final BiFunction<? super I, Throwable, Uni<? extends O>> mapper;

    public UniOnItemOrFailureFlatMap(Uni<I> upstream,
            BiFunction<? super I, Throwable, Uni<? extends O>> mapper) {
        super(upstream);
        this.mapper = mapper;
    }

    public static <I, O> void invokeAndSubstitute(BiFunction<? super I, Throwable, Uni<? extends O>> mapper,
            I item,
            Throwable failure,
            UniSubscriber<? super O> subscriber,
            UniOnItemTransformToUni.FlatMapSubscription flatMapSubscription) {
        Uni<? extends O> outcome;
        try {
            outcome = mapper.apply(item, failure);
            // We cannot call onItem here, as if onItem would throw an exception
            // it would be caught and onFailure would be called. This would be illegal.
        } catch (Throwable e) { // NOSONAR
            if (failure != null) {
                subscriber.onFailure(new CompositeException(failure, e));
            } else {
                subscriber.onFailure(e);
            }
            return;
        }

        handleInnerSubscription(subscriber, flatMapSubscription, outcome);
    }

    @Override
    protected void subscribing(UniSubscriber<? super O> subscriber) {
        UniOnItemTransformToUni.FlatMapSubscription flatMapSubscription = new UniOnItemTransformToUni.FlatMapSubscription();
        // Subscribe to the source.
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<I, O>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                flatMapSubscription.setInitialUpstream(subscription);
                subscriber.onSubscribe(flatMapSubscription);
            }

            @Override
            public void onItem(I item) {
                invokeAndSubstitute(mapper, item, null, subscriber, flatMapSubscription);
            }

            @Override
            public void onFailure(Throwable failure) {
                invokeAndSubstitute(mapper, null, failure, subscriber, flatMapSubscription);
            }
        });
    }
}
