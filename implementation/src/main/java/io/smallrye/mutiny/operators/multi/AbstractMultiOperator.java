package io.smallrye.mutiny.operators.multi;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.operators.AbstractMulti;

/**
 * Abstract base class for operators that take an upstream source {@link Multi}.
 *
 * @param <I> the upstream value type / input type
 * @param <O> the output value type / produced type
 */
public abstract class AbstractMultiOperator<I, O> extends AbstractMulti<O> implements Multi<O> {

    /**
     * The upstream {@link Multi}.
     */
    protected final Multi<? extends I> upstream;

    /**
     * Creates a new {@link AbstractMultiOperator} with the passed {@link Multi} as upstream.
     *
     * @param upstream the upstream, must not be {@code null}
     */
    public AbstractMultiOperator(Multi<? extends I> upstream) {
        this.upstream = ParameterValidation.nonNull(upstream, "upstream");
    }

    /**
     * @return the upstream.
     */
    public Multi<? extends I> upstream() {
        return upstream;
    }
}
