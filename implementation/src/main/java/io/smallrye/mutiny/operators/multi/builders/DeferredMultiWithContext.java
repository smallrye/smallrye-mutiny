package io.smallrye.mutiny.operators.multi.builders;

import java.util.function.Function;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class DeferredMultiWithContext<T> extends AbstractMulti<T> {

    private final Function<Context, Multi<? extends T>> mapper;

    public DeferredMultiWithContext(Function<Context, Multi<? extends T>> mapper) {
        this.mapper = mapper;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        Context context;
        if (downstream instanceof ContextSupport) {
            context = ((ContextSupport) downstream).context();
        } else {
            context = Context.empty();
        }

        Multi<? extends T> multi;
        try {
            multi = mapper.apply(context);
            if (multi == null) {
                throw new NullPointerException(ParameterValidation.MAPPER_RETURNED_NULL);
            }
        } catch (Throwable failure) {
            Subscriptions.fail(downstream, failure);
            return;
        }

        multi.subscribe(Infrastructure.onMultiSubscription(multi, downstream));
    }
}
