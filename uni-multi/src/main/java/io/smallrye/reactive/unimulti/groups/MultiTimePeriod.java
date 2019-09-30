package io.smallrye.reactive.unimulti.groups;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.infrastructure.Infrastructure;
import io.smallrye.reactive.unimulti.operators.AbstractMulti;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.validate;

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
