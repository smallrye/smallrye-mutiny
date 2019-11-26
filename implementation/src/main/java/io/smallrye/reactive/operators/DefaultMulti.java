package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import org.reactivestreams.Publisher;

public class DefaultMulti<T> extends AbstractMulti<T> {

    private final Publisher<T> delegate;

    public DefaultMulti(Publisher<T> delegate) {
        this.delegate = nonNull(delegate, "delegate");
    }

    @Override
    protected Publisher<T> publisher() {
        return delegate;
    }
}
