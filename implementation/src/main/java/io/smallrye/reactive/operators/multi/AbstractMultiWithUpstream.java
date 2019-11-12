package io.smallrye.reactive.operators.multi;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.operators.AbstractMulti;

/**
 * Abstract base class for operators that take an upstream source {@link Multi}.
 *
 * @param <I> the upstream value type / input type
 * @param <O> the output value type / produced type
 */
public abstract class AbstractMultiWithUpstream<I, O> extends AbstractMulti<O> implements Multi<O> {

    /**
     * The upstream {@link Multi}.
     */
    protected final Multi<? extends I> upstream;

    /**
     * Creates a new {@link AbstractMultiWithUpstream} with the passed {@link Multi} as upstream.
     *
     * @param upstream the upstream, must not be {@code null}
     */
    public AbstractMultiWithUpstream(Multi<? extends I> upstream) {
        this.upstream = ParameterValidation.nonNull(upstream, "upstream");
    }

    public Multi<? extends I> upstream() {
        return upstream;
    }

    @Override
    protected Publisher<O> publisher() {
        return this;
    }
}
