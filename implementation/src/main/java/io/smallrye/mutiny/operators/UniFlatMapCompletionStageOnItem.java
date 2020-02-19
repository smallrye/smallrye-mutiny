package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.MAPPER_RETURNED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniFlatMapCompletionStageOnItem<I, O> extends UniOperator<I, O> {

    private final Function<? super I, ? extends CompletionStage<? extends O>> mapper;

    public UniFlatMapCompletionStageOnItem(Uni<I> upstream,
            Function<? super I, ? extends CompletionStage<? extends O>> mapper) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "mapper");
    }

    private static <I, O> void invokeAndSubstitute(Function<? super I, ? extends CompletionStage<? extends O>> mapper,
            I input,
            UniSerializedSubscriber<? super O> subscriber,
            UniFlatMapOnItem.FlatMapSubscription flatMapSubscription) {
        CompletionStage<? extends O> outcome;
        try {
            outcome = mapper.apply(input);
        } catch (Throwable e) {
            subscriber.onFailure(e);
            return;
        }

        if (outcome == null) {
            subscriber.onFailure(new NullPointerException(MAPPER_RETURNED_NULL));
        } else {
            UniSubscriber<O> delegate = new UniDelegatingSubscriber<O, O>(subscriber) {
                @Override
                public void onSubscribe(UniSubscription secondSubscription) {
                    flatMapSubscription.replace(secondSubscription);
                }
            };

            Uni.createFrom().completionStage(outcome).subscribe().withSubscriber(delegate);
        }
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        UniFlatMapOnItem.FlatMapSubscription flatMapSubscription = new UniFlatMapOnItem.FlatMapSubscription();
        // Subscribe to the source.
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<I, O>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                flatMapSubscription.setInitialUpstream(subscription);
                subscriber.onSubscribe(flatMapSubscription);
            }

            @Override
            public void onItem(I item) {
                invokeAndSubstitute(mapper, item, subscriber, flatMapSubscription);
            }

        });
    }
}
