package io.smallrye.reactive.groups;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.adapt.UniAdaptTo;
import io.smallrye.reactive.adapt.UniAdapter;
import io.smallrye.reactive.operators.UniToPublisher;
import org.reactivestreams.Publisher;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniAdapt<T> {

    private final Uni<T> upstream;

    public UniAdapt(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Transforms this {@link Uni} into an instance of the given class. The transformations acts as follows:
     * <ol>
     * <li>If this is an instance of O - return this</li>
     * <li>If there is on the classpath, an implementation of {@link UniAdapter}
     * for the type O, the adapter is used (invoking {@link UniAdapter#adaptTo(Uni)})</li>
     * <li>If O has a {@code fromPublisher} method, this method is called with a {@link Publisher} produced
     * using {@link #toPublisher()}</li>
     * <li>If O has a {@code instance} method, this method is called with a {@link Publisher} produced
     * using {@link #toPublisher()}</li>
     * </ol>
     *
     * @param clazz the output class
     * @param <O>   the produced type
     * @return an instance of O
     * @throws RuntimeException if the conversion fails.
     */
    public <O> O to(Class<O> clazz) {
        return new UniAdaptTo<>(upstream, nonNull(clazz, "clazz")).adapt();
    }

    public Publisher<T> toPublisher() {
        return UniToPublisher.adapt(upstream);
    }

}
