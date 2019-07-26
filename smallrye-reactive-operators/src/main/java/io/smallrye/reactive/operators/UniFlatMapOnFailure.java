package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.subscription.UniSubscription;

import java.util.function.Function;
import java.util.function.Predicate;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.operators.UniFlatMapOnResult.invokeAndSubstitute;

public class UniFlatMapOnFailure<I> extends UniOperator<I, I> {


    private final Function<? super Throwable, ? extends Uni<? extends I>> mapper;
    private final Predicate<? super Throwable> predicate;


    public UniFlatMapOnFailure(Uni<I> upstream,
                               Predicate<? super Throwable> predicate,
                               Function<? super Throwable, ? extends Uni<? extends I>> mapper) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "mapper");
        this.predicate = nonNull(predicate, "predicate");
    }

    @Override
    public void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        UniFlatMapOnResult.FlatMapSubscription flatMapSubscription = new UniFlatMapOnResult.FlatMapSubscription();
        // Subscribe to the source.
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<I, I>(subscriber) {
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
                    subscriber.onFailure(e);
                    return;
                }

                if (test) {
                    invokeAndSubstitute(mapper, failure, subscriber, flatMapSubscription);
                } else {
                    subscriber.onFailure(failure);
                }

            }

        });
    }
}
