package io.smallrye.reactive.groups;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.infrastructure.Infrastructure;
import io.smallrye.reactive.operators.multi.builders.IntervalMulti;

public class MultiTimePeriod {

    private Duration initialDelay;
    private ScheduledExecutorService executor = Infrastructure.getDefaultWorkerPool();

    public MultiTimePeriod startingAfter(Duration duration) {
        this.initialDelay = validate(duration, "duration");
        return this;
    }

    public MultiTimePeriod onExecutor(ScheduledExecutorService executor) {
        this.executor = nonNull(executor, "executor");
        return this;
    }

    public Multi<Long> every(Duration duration) {
        validate(duration, "duration");
        if (initialDelay != null) {
            return new IntervalMulti(initialDelay, duration, executor);
        } else {
            return new IntervalMulti(duration, executor);
        }
    }

}
