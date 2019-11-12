package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import org.reactivestreams.Publisher;

import io.reactivex.Flowable;

public class DefaultMulti<T> extends AbstractMulti<T> {

    private final Flowable<T> delegate;

    public DefaultMulti(Flowable<T> delegate) {
        this.delegate = nonNull(delegate, "delegate");
    }

    @Override
    protected Publisher<T> publisher() {
        return delegate;
    }
}
