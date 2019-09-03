package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class MultiScanWithInitialState<T, S> extends MultiOperator<T, S> {

    private final Supplier<S> initialStateProducer;
    private final BiFunction<S, ? super T, S> scanner;

    public MultiScanWithInitialState(Multi<T> upstream, Supplier<S> initialStateProducer,
            BiFunction<S, ? super T, S> scanner) {
        super(upstream);
        this.initialStateProducer = initialStateProducer;
        this.scanner = scanner;
    }

    @Override
    protected Flowable<S> flowable() {
        return upstreamAsFlowable().scanWith(initialStateProducer::get, scanner::apply);
    }

}
