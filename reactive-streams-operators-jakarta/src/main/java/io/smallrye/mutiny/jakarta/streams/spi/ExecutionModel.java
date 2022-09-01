package io.smallrye.mutiny.jakarta.streams.spi;

import java.util.function.UnaryOperator;

import io.smallrye.mutiny.Multi;

@FunctionalInterface
public interface ExecutionModel extends UnaryOperator<Multi> {

}
