package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Uni;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiCreateFromCompletionStage<T> extends MultiOperator<Void, T> {
    private final Flowable<T> publisher;

    public MultiCreateFromCompletionStage(Supplier<? extends CompletionStage<? extends T>> supplier) {
        super(null);
        this.publisher = Flowable.fromPublisher(
                Uni.createFrom().completionStage(nonNull(supplier, "supplier")).adapt().toPublisher()
        );
    }

    @Override
    protected Flowable<T> flowable() {
        return publisher;
    }
}
