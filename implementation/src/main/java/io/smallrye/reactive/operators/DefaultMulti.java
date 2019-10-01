package io.smallrye.reactive.operators;

import io.reactivex.Flowable;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class DefaultMulti<T> extends AbstractMulti<T> {

    private final Flowable<T> delegate;

    public DefaultMulti(Flowable<T> delegate) {
        this.delegate = nonNull(delegate, "delegate");
    }

    @Override
    protected Flowable<T> flowable() {
        return delegate;
    }
}
