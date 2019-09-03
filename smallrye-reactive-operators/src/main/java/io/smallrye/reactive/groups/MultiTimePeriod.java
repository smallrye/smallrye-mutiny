package io.smallrye.reactive.groups;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.infrastructure.Infrastructure;
import io.smallrye.reactive.operators.AbstractMulti;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.helpers.ParameterValidation.validate;

public class MultiTimePeriod {

    private Duration initialDelay;
    private Executor executor = Infrastructure.getDefaultExecutor();

    public MultiTimePeriod startingAfter(Duration duration) {
        this.initialDelay = validate(duration, "duration");
        return this;
    }

    public MultiTimePeriod onExecutor(Executor executor) {
        this.executor = nonNull(executor, "executor");
        return this;
    }

    public Multi<Long> every(Duration duration) {
        validate(duration, "duration");
        return new AbstractMulti<Long>() {
            @Override
            protected Flowable<Long> flowable() {
                if (initialDelay == null) {
                    return Flowable.interval(duration.toMillis(), TimeUnit.MILLISECONDS, Schedulers.from(executor));
                } else {
                    return Flowable.interval(initialDelay.toMillis(), duration.toMillis(), TimeUnit.MILLISECONDS,
                            Schedulers.from(executor));
                }

            }
        };
    }

}
