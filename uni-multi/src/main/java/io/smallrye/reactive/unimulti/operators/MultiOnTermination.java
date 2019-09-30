package io.smallrye.reactive.unimulti.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Multi;

import java.util.function.BiConsumer;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

public class MultiOnTermination<T> extends MultiOperator<T, T> {
    private final BiConsumer<Throwable, Boolean> consumer;

    public MultiOnTermination(Multi<T> upstream, BiConsumer<Throwable, Boolean> consumer) {
        super(nonNull(upstream, "upstream"));
        this.consumer = nonNull(consumer, "consumer");
    }

    @Override
    protected Flowable<T> flowable() {
        // Important: callback order:
        // onError must be first because if onCompletion or onCancel throws an exception, it should be handled
        // downstream, and not calling the callback again.
        return upstreamAsFlowable()
                .doOnError(fail -> consumer.accept(fail, false))
                .doOnComplete(() -> consumer.accept(null, false))
                .doOnCancel(() -> consumer.accept(null, true));
    }
}
