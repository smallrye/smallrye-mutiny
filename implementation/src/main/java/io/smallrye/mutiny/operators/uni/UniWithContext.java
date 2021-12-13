package io.smallrye.mutiny.operators.uni;

import java.util.function.BiFunction;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class UniWithContext<I, O> extends UniOperator<I, O> {

    private final Uni<I> upstream;
    private final BiFunction<Uni<I>, Context, Uni<O>> builder;

    public UniWithContext(Uni<I> upstream, BiFunction<Uni<I>, Context, Uni<O>> builder) {
        super(upstream);
        this.upstream = upstream;
        this.builder = builder;
    }

    @Override
    public void subscribe(UniSubscriber<? super O> downstream) {
        Context context = downstream.context();
        Uni<O> uni;
        try {
            uni = builder.apply(upstream, context);
            if (uni == null) {
                downstream.onSubscribe(EmptyUniSubscription.DONE);
                downstream.onFailure(new NullPointerException("The builder function returned null"));
                return;
            }
        } catch (Throwable err) {
            downstream.onSubscribe(EmptyUniSubscription.DONE);
            downstream.onFailure(err);
            return;
        }
        Uni<O> pipelineWithContext = Infrastructure.onUniCreation(uni);
        AbstractUni.subscribe(pipelineWithContext, downstream);
    }
}
