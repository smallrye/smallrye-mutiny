package io.smallrye.reactive.unimulti.operators;

import io.reactivex.Flowable;
import io.reactivex.flowables.GroupedFlowable;
import io.smallrye.reactive.unimulti.GroupedMulti;
import io.smallrye.reactive.unimulti.helpers.ParameterValidation;

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
