package io.smallrye.reactive.operators;

import io.reactivex.flowables.GroupedFlowable;
import io.smallrye.reactive.GroupedMulti;
import io.smallrye.reactive.helpers.ParameterValidation;
import org.reactivestreams.Publisher;

public class DefaultGroupedMulti<K, T> extends AbstractMulti<T> implements GroupedMulti<K, T> {

    private final GroupedFlowable<? extends K, ? extends T> delegate;

    public DefaultGroupedMulti(GroupedFlowable<? extends K, ? extends T> delegate) {
        this.delegate = ParameterValidation.nonNull(delegate, "delegate");
    }

    @Override
    protected Publisher<T> publisher() {
        return delegate.map(t -> (T) t);
    }

    @Override
    public K key() {
        return delegate.getKey();
    }
}
