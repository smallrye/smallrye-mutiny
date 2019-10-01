package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.reactivex.flowables.GroupedFlowable;
import io.smallrye.reactive.GroupedMulti;
import io.smallrye.reactive.helpers.ParameterValidation;

public class DefaultGroupedMulti<K, T> extends AbstractMulti<T> implements GroupedMulti<K, T> {

    private final GroupedFlowable<? extends K, ? extends T> delegate;

    public DefaultGroupedMulti(GroupedFlowable<? extends K, ? extends T> delegate) {
        this.delegate = ParameterValidation.nonNull(delegate, "delegate");
    }

    @Override
    protected Flowable<T> flowable() {
        return delegate.map(t -> (T) t);
    }

    @Override
    public K key() {
        return delegate.getKey();
    }
}
