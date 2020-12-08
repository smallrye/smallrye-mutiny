package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Function;
import java.util.function.Predicate;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnFailureFlatMap<I> extends UniOperator<I, I> {

    private final Function<? super Throwable, Uni<? extends I>> mapper;
    private final Predicate<? super Throwable> predicate;

    public UniOnFailureFlatMap(Uni<I> upstream,
            Predicate<? super Throwable> predicate,
            Function<? super Throwable, Uni<? extends I>> mapper) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "mapper");
        this.predicate = nonNull(predicate, "predicate");
    }

    @Override
    protected void subscribing(UniSubscriber<? super I> subscriber) {
        UniOnItemTransformToUni.FlatMapSubscription flatMapSubscription = new UniOnItemTransformToUni.FlatMapSubscription();
        // Subscribe to the source.
        AbstractUni.subscribe(upstream(), new UniDelegatingSubscriber<I, I>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                flatMapSubscription.setInitialUpstream(subscription);
                subscriber.onSubscribe(flatMapSubscription);
            }

            @Override
            public void onFailure(Throwable failure) {
                boolean test;
                try {
                    test = predicate.test(failure);
                } catch (RuntimeException e) {
                    subscriber.onFailure(new CompositeException(failure, e));
                    return;
                }

                if (test) {
                    UniOnItemTransformToUni.invokeAndSubstitute(mapper, failure, subscriber, flatMapSubscription);
                } else {
                    subscriber.onFailure(failure);
                }

            }

        });
    }
}
