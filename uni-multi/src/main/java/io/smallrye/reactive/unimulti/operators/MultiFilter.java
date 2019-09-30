package io.smallrye.reactive.unimulti.operators;

import java.util.function.Predicate;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Multi;

public class MultiFilter<T> extends MultiOperator<T, T> {
    private final Predicate<? super T> predicate;

    public MultiFilter(Multi<T> upstream, Predicate<? super T> predicate) {
        super(upstream);
        this.predicate = predicate;
    }

    @Override
    protected Flowable<T> flowable() {
        return upstreamAsFlowable().filter(predicate::test);
    }
}
