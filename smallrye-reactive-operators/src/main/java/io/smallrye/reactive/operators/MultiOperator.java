package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

public abstract class MultiOperator<I, O> extends AbstractMulti<O> {

    private final Multi<? extends I> upstream;

    public MultiOperator(Multi<? extends I> upstream) {
        // NOTE: upstream can be null. It's null when creating a "source".
        this.upstream = upstream;
    }

    public Multi<? extends I> upstream() {
        return upstream;
    }

    @SuppressWarnings("unchecked")
    protected Flowable<? extends I> upstreamAsFlowable() {
        if (upstream instanceof AbstractMulti) {
            ((AbstractMulti<? extends I>) upstream).flowable();
        }
        return Flowable.fromPublisher(upstream);
    }


}
