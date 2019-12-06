package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.*;

import io.smallrye.mutiny.Multi;

/**
 * Configures a flatMap operator.
 *
 * @param <I> the type of item emitted by the upstream {@link Multi}.
 */
public class MultiFlatMap<I> {

    private final Multi<I> upstream;

    MultiFlatMap(Multi<I> upstream) {
        this.upstream = upstream;
    }

}
