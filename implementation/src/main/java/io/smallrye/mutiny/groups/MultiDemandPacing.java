package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.ScheduledExecutorService;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.operators.multi.MultiDemandPacer;
import io.smallrye.mutiny.subscription.DemandPacer;

public class MultiDemandPacing<T> {

    private final AbstractMulti<T> upstream;
    private ScheduledExecutorService executor = Infrastructure.getDefaultWorkerPool();

    public MultiDemandPacing(AbstractMulti<T> upstream) {
        this.upstream = upstream;
    }

    /**
     * Sets the executor to use for issuing demand requests.
     * The default is to use {@link Infrastructure#getDefaultWorkerPool()}.
     *
     * @param executor the executor, must not be {@code null}
     * @return this group
     */
    @CheckReturnValue
    public MultiDemandPacing<T> on(ScheduledExecutorService executor) {
        this.executor = nonNull(executor, "executor");
        return this;
    }

    /**
     * Sets the demand pacer and return the new {@link Multi}.
     *
     * @param pacer the pacer, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> using(DemandPacer pacer) {
        return Infrastructure.onMultiCreation(new MultiDemandPacer<>(upstream, executor, nonNull(pacer, "pacer")));
    }
}
