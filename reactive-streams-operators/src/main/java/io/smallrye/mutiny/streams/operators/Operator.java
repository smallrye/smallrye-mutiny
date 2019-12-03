package io.smallrye.mutiny.streams.operators;

import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

public class Operator<T extends Stage> implements Predicate<Stage> {
    private Class<T> clazz;

    Operator(Class<T> clazz) {
        this.clazz = Objects.requireNonNull(clazz);
    }

    public boolean test(Stage s) {
        return clazz.isAssignableFrom(s.getClass());
    }
}
