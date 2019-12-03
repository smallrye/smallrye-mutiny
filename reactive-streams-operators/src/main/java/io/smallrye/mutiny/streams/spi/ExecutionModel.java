package io.smallrye.mutiny.streams.spi;

import java.util.function.UnaryOperator;

import io.smallrye.mutiny.Multi;

@FunctionalInterface
public interface ExecutionModel extends UnaryOperator<Multi> {

}
