package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

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
