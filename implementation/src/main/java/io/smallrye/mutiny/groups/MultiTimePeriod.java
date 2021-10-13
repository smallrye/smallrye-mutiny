package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.CheckReturnValue;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.builders.IntervalMulti;

public class MultiTimePeriod {

    private Duration initialDelay;
    private ScheduledExecutorService executor = Infrastructure.getDefaultWorkerPool();

    @CheckReturnValue
    public MultiTimePeriod startingAfter(Duration duration) {
        this.initialDelay = validate(duration, "duration");
        return this;
    }

    @CheckReturnValue
    public MultiTimePeriod onExecutor(ScheduledExecutorService executor) {
        this.executor = nonNull(executor, "executor");
        return this;
    }

    @CheckReturnValue
    public Multi<Long> every(Duration duration) {
        validate(duration, "duration");
        if (initialDelay != null) {
            return Infrastructure.onMultiCreation(new IntervalMulti(initialDelay, duration, executor));
        } else {
            return Infrastructure.onMultiCreation(new IntervalMulti(duration, executor));
        }
    }

}
