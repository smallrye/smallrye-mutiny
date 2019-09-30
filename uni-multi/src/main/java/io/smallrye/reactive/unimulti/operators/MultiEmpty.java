package io.smallrye.reactive.unimulti.operators;

import io.reactivex.Flowable;

public class MultiEmpty<T> extends AbstractMulti<T> {
    public static final MultiEmpty<Object> INSTANCE = new MultiEmpty<>();

    private MultiEmpty() {
        // avoid direct instantiation.
    }

    @Override
    protected Flowable<T> flowable() {
        return Flowable.empty();
    }
}
