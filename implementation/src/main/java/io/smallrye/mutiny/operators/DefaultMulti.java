package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

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
