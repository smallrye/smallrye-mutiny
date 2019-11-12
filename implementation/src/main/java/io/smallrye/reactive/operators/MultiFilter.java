package io.smallrye.reactive.operators;

import java.util.function.Predicate;

import io.smallrye.reactive.Multi;
import org.reactivestreams.Publisher;

public class MultiFilter<T> extends MultiOperator<T, T> {
    private final Predicate<? super T> predicate;

    public MultiFilter(Multi<T> upstream, Predicate<? super T> predicate) {
        super(upstream);
        this.predicate = predicate;
    }

    @Override
    protected Publisher<T> publisher() {
        return upstreamAsFlowable().filter(predicate::test);
    }
}
