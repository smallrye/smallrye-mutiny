package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.MAPPER_RETURNED_NULL;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;
import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.MultiOperator;

public class MultiOnFailureTransform<T> extends MultiOperator<T, T> {
    private final Predicate<? super Throwable> predicate;
    private final Function<? super Throwable, ? extends Throwable> mapper;

    public MultiOnFailureTransform(Multi<T> upstream, Predicate<? super Throwable> predicate,
            Function<? super Throwable, ? extends Throwable> mapper) {
        super(upstream);
        this.predicate = predicate == null ? x -> true : predicate;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber, "The subscriber must not be `null`");
        Function<? super Throwable, ? extends Publisher<? extends T>> next = failure -> {
            if (predicate.test(failure)) {
                Throwable throwable = mapper.apply(failure);
                if (throwable == null) {
                    return Multi.createFrom().failure(new NullPointerException(MAPPER_RETURNED_NULL));
                } else {
                    return Multi.createFrom().failure(throwable);
                }
            }
            return Multi.createFrom().failure(failure);
        };
        Multi<T> op = Infrastructure.onMultiCreation(new MultiOnFailureResumeOp<>(upstream(), next));
        op.subscribe(subscriber);
    }
}
