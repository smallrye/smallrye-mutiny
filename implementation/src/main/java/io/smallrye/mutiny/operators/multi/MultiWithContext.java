package io.smallrye.mutiny.operators.multi;

import java.util.function.BiFunction;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiWithContext<I, O> extends AbstractMultiOperator<I, O> {

    private final BiFunction<Multi<I>, Context, Multi<O>> builder;

    public MultiWithContext(Multi<? extends I> upstream, BiFunction<Multi<I>, Context, Multi<O>> builder) {
        super(upstream);
        this.builder = builder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribe(MultiSubscriber<? super O> downstream) {
        ParameterValidation.nonNull(downstream, "downstream");

        Context context;
        if (downstream instanceof ContextSupport) {
            context = ((ContextSupport) downstream).context();
        } else {
            context = Context.empty();
        }

        Multi<O> multi;
        try {
            multi = builder.apply((Multi<I>) upstream, context);
            if (multi == null) {
                downstream.onSubscribe(Subscriptions.CANCELLED);
                downstream.onFailure(new NullPointerException("The builder function returned null"));
                return;
            }
        } catch (Throwable err) {
            downstream.onSubscribe(Subscriptions.CANCELLED);
            downstream.onFailure(err);
            return;
        }

        Multi<O> pipelineWithContext = Infrastructure.onMultiCreation(multi);
        pipelineWithContext.subscribe().withSubscriber(downstream);
    }
}
