package io.smallrye.mutiny.operators;

import io.smallrye.mutiny.Multi;

public abstract class MultiOperator<I, O> extends AbstractMulti<O> {

    private final Multi<I> upstream;

    public MultiOperator(Multi<I> upstream) {
        // NOTE: upstream can be null. It's null when creating a "source".
        this.upstream = upstream;
    }

    public Multi<I> upstream() {
        return upstream;
    }

}
