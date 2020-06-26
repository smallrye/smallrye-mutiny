package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.operators.UniOnItemFlatMap.handleInnerSubscription;

import java.util.function.BiFunction;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnItemOrFailureFlatMap<I, O> extends UniOperator<I, O> {

    private final BiFunction<? super I, Throwable, ? extends Uni<? extends O>> mapper;

    public UniOnItemOrFailureFlatMap(Uni<I> upstream,
            BiFunction<? super I, Throwable, ? extends Uni<? extends O>> mapper) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "mapper");
    }

    public static <I, O> void invokeAndSubstitute(BiFunction<? super I, Throwable, ? extends Uni<? extends O>> mapper,
            I item,
            Throwable failure,
            UniSerializedSubscriber<? super O> subscriber,
            UniOnItemFlatMap.FlatMapSubscription flatMapSubscription) {
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
    protected void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        UniOnItemFlatMap.FlatMapSubscription flatMapSubscription = new UniOnItemFlatMap.FlatMapSubscription();
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
