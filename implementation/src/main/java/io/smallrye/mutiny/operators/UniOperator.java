package io.smallrye.mutiny.operators;

import io.smallrye.mutiny.Uni;

public abstract class UniOperator<I, O> extends AbstractUni<O> {

    private final Uni<? extends I> upstream;

    public UniOperator(Uni<? extends I> upstream) {
        // NOTE: upstream can be null. It's null when creating a "source".
        this.upstream = upstream;
    }

    public Uni<? extends I> upstream() {
        return upstream;
    }

}
