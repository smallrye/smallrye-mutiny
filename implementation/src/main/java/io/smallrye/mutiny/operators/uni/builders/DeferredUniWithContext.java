package io.smallrye.mutiny.operators.uni.builders;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.DONE;

import java.util.function.Function;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class DeferredUniWithContext<T> extends AbstractUni<T> {

    private final Function<Context, Uni<? extends T>> mapper;

    public DeferredUniWithContext(Function<Context, Uni<? extends T>> mapper) {
        this.mapper = mapper;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        Uni<? extends T> uni;
        try {
            uni = mapper.apply(subscriber.context());
            if (uni == null) {
                subscriber.onSubscribe(DONE);
                subscriber.onFailure(new NullPointerException(ParameterValidation.MAPPER_RETURNED_NULL));
                return;
            }
        } catch (Throwable failure) {
            subscriber.onSubscribe(DONE);
            subscriber.onFailure(failure);
            return;
        }

        AbstractUni.subscribe(uni, subscriber);
    }
}
