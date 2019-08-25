package io.smallrye.reactive.groups;

import io.smallrye.reactive.Multi;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiOverflow<T> {
    private final Multi<T> upstream;

    public MultiOverflow(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    // TODO drop(), fail()...
}
