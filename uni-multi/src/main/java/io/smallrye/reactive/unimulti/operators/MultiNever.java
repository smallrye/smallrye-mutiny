package io.smallrye.reactive.unimulti.operators;

import io.reactivex.Flowable;

public class MultiNever<T> extends AbstractMulti<T> {
    public static final MultiNever<Object> INSTANCE = new MultiNever<>();

    private MultiNever() {
        // avoid direct instantiation.
    }

    @Override
    protected Flowable<T> flowable() {
        return Flowable.never();
    }
}
