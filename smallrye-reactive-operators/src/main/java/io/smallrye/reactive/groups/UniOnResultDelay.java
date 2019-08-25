package io.smallrye.reactive.groups;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.helpers.Infrastructure;
import io.smallrye.reactive.operators.UniDelayOnItem;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

public class UniOnResultDelay<T> {

    private final Uni<T> upstream;
    private final ScheduledExecutorService executor;

    public UniOnResultDelay(Uni<T> upstream, ScheduledExecutorService executor) {
        this.upstream = upstream;
        this.executor = executor == null ? Infrastructure.getDefaultExecutor() : executor;
    }

    public UniOnResultDelay<T> onExecutor(ScheduledExecutorService executor) {
        return new UniOnResultDelay<>(upstream, executor);
    }

    public Uni<T> by(Duration duration) {
        return new UniDelayOnItem<>(upstream, duration, executor);
    }

    public Uni<T> until(Function<? super T, ? extends Uni<?>> function) {
        throw new UnsupportedOperationException("to be implemented");
    }

}
