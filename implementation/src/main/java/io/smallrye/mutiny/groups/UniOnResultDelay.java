package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniDelayOnItem;
import io.smallrye.mutiny.operators.UniDelayUntil;

public class UniOnResultDelay<T> {

    private final Uni<T> upstream;
    private ScheduledExecutorService executor;

    public UniOnResultDelay(Uni<T> upstream, ScheduledExecutorService executor) {
        this.upstream = upstream;
        this.executor = executor == null ? Infrastructure.getDefaultWorkerPool() : executor;
    }

    public UniOnResultDelay<T> onExecutor(ScheduledExecutorService executor) {
        this.executor = nonNull(executor, "executor");
        return this;
    }

    public Uni<T> by(Duration duration) {
        return Infrastructure.onUniCreation(new UniDelayOnItem<>(upstream, duration, executor));
    }

    public Uni<T> until(Function<? super T, ? extends Uni<?>> function) {
        return Infrastructure.onUniCreation(new UniDelayUntil<>(upstream, function, executor));

    }

}
