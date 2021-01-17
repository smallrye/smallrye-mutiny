package io.smallrye.mutiny.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Functions;

public class ContextPropagationDecoratorTest {

    Runnable runnable = () -> {
    };
    BiConsumer<Integer, Integer> biConsumer = (i1, i2) -> {
    };
    BiFunction<Integer, Integer, Integer> biFunction = Integer::sum;
    BooleanSupplier booleanSupplier = () -> true;
    Consumer<Integer> consumer = i -> {
    };
    Predicate<Integer> predicate = i -> true;
    Supplier<Integer> supplier = () -> 1;
    Function<Integer, Integer> function = i -> i + 1;

    Functions.TriConsumer<Integer, Integer, Integer> triConsumer = (a, b, c) -> {
    };
    BinaryOperator<Integer> binOp = Integer::sum;
    LongConsumer longConsumer = l -> {
    };

    Functions.Function3<Integer, Integer, Integer, Integer> function3 = (a, b, c) -> 0;
    Functions.Function4<Integer, Integer, Integer, Integer, Integer> function4 = (a, b, c, d) -> 0;
    Functions.Function5<Integer, Integer, Integer, Integer, Integer, Integer> function5 = (a, b, c, d, e) -> 0;
    Functions.Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer> function6 = (a, b, c, d, e, f) -> 0;
    Functions.Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> function7 = (a, b, c, d, e, f,
            g) -> 0;
    Functions.Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> function8 = (a, b, c,
            d, e,
            f, g, h) -> 0;
    Functions.Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> function9 = (
            a, b,
            c, d, e, f, g, h, i) -> 0;

    @AfterEach
    public void cleanup() {
        Infrastructure.clearInterceptors();
        MyContext.clear();
    }

    @Test
    public void testMultipleDecorationsWithoutContext() {
        Runnable runnableCtx = Infrastructure.decorate(runnable);
        assertThat(runnableCtx).isNotSameAs(runnable);
        assertThat(Infrastructure.decorate(runnableCtx)).isSameAs(runnableCtx);

        BiConsumer<Integer, Integer> biConsumerCtx = Infrastructure.decorate(biConsumer);
        assertThat(biConsumerCtx).isNotSameAs(biConsumer);
        assertThat(Infrastructure.decorate(biConsumerCtx)).isSameAs(biConsumerCtx);

        BiFunction<Integer, Integer, Integer> biFunctionCtx = Infrastructure.decorate(biFunction);
        assertThat(biFunctionCtx).isNotSameAs(biFunction);
        assertThat(Infrastructure.decorate(biFunctionCtx)).isSameAs(biFunctionCtx);

        BooleanSupplier booleanSupplierCtx = Infrastructure.decorate(booleanSupplier);
        assertThat(booleanSupplierCtx).isNotSameAs(booleanSupplier);
        assertThat(Infrastructure.decorate(booleanSupplierCtx)).isSameAs(booleanSupplierCtx);

        Consumer<Integer> consumerCtx = Infrastructure.decorate(consumer);
        assertThat(consumerCtx).isNotSameAs(consumer);
        assertThat(Infrastructure.decorate(consumerCtx)).isSameAs(consumerCtx);

        Predicate<Integer> predicateCtx = Infrastructure.decorate(predicate);
        assertThat(predicateCtx).isNotSameAs(predicate);
        assertThat(Infrastructure.decorate(predicateCtx)).isSameAs(predicateCtx);

        Supplier<Integer> supplierCtx = Infrastructure.decorate(supplier);
        assertThat(supplierCtx).isNotSameAs(supplier);
        assertThat(Infrastructure.decorate(supplierCtx)).isSameAs(supplierCtx);

        Function<Integer, Integer> functionCtx = Infrastructure.decorate(function);
        assertThat(functionCtx).isNotSameAs(function);
        assertThat(Infrastructure.decorate(functionCtx)).isSameAs(functionCtx);

        Functions.TriConsumer<Integer, Integer, Integer> triConsumerCtx = Infrastructure.decorate(triConsumer);
        assertThat(triConsumerCtx).isNotSameAs(triConsumer);
        assertThat(Infrastructure.decorate(triConsumerCtx)).isSameAs(triConsumerCtx);

        BinaryOperator<Integer> binaryOperatorCtx = Infrastructure.decorate(binOp);
        assertThat(binaryOperatorCtx).isNotSameAs(binOp);
        assertThat(Infrastructure.decorate(binaryOperatorCtx)).isSameAs(binaryOperatorCtx);

        LongConsumer longConsumerCtx = Infrastructure.decorate(longConsumer);
        assertThat(longConsumerCtx).isNotSameAs(longConsumer);
        assertThat(Infrastructure.decorate(longConsumerCtx)).isSameAs(longConsumerCtx);

        Functions.Function3<Integer, Integer, Integer, Integer> function3Ctx = Infrastructure.decorate(function3);
        assertThat(function3Ctx).isNotSameAs(function3);
        assertThat(Infrastructure.decorate(function3Ctx)).isSameAs(function3Ctx);

        Functions.Function4<Integer, Integer, Integer, Integer, Integer> function4Ctx = Infrastructure.decorate(function4);
        assertThat(function4Ctx).isNotSameAs(function4);
        assertThat(Infrastructure.decorate(function4Ctx)).isSameAs(function4Ctx);

        Functions.Function5<Integer, Integer, Integer, Integer, Integer, Integer> function5Ctx = Infrastructure
                .decorate(function5);
        assertThat(function5Ctx).isNotSameAs(function5);
        assertThat(Infrastructure.decorate(function5Ctx)).isSameAs(function5Ctx);

        Functions.Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer> function6Ctx = Infrastructure
                .decorate(function6);
        assertThat(function6Ctx).isNotSameAs(function6);
        assertThat(Infrastructure.decorate(function6Ctx)).isSameAs(function6Ctx);

        Functions.Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> function7Ctx = Infrastructure
                .decorate(function7);
        assertThat(function7Ctx).isNotSameAs(function7);
        assertThat(Infrastructure.decorate(function7Ctx)).isSameAs(function7Ctx);

        Functions.Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> function8Ctx = Infrastructure
                .decorate(function8);
        assertThat(function8Ctx).isNotSameAs(function8);
        assertThat(Infrastructure.decorate(function8Ctx)).isSameAs(function8Ctx);

        Functions.Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> function9Ctx = Infrastructure
                .decorate(function9);
        assertThat(function9Ctx).isNotSameAs(function9);
        assertThat(Infrastructure.decorate(function9Ctx)).isSameAs(function9Ctx);
    }

    @Test
    public void testMultipleDecorationsWithContext() {
        MyContext.init();
        Infrastructure.reload();

        Runnable runnableCtx = Infrastructure.decorate(runnable);
        assertThat(runnableCtx).isNotSameAs(runnable);
        assertThat(Infrastructure.decorate(runnableCtx)).isSameAs(runnableCtx);

        BiConsumer<Integer, Integer> biConsumerCtx = Infrastructure.decorate(biConsumer);
        assertThat(biConsumerCtx).isNotSameAs(biConsumer);
        assertThat(Infrastructure.decorate(biConsumerCtx)).isSameAs(biConsumerCtx);

        BiFunction<Integer, Integer, Integer> biFunctionCtx = Infrastructure.decorate(biFunction);
        assertThat(biFunctionCtx).isNotSameAs(biFunction);
        assertThat(Infrastructure.decorate(biFunctionCtx)).isSameAs(biFunctionCtx);

        BooleanSupplier booleanSupplierCtx = Infrastructure.decorate(booleanSupplier);
        assertThat(booleanSupplierCtx).isNotSameAs(booleanSupplier);
        assertThat(Infrastructure.decorate(booleanSupplierCtx)).isSameAs(booleanSupplierCtx);

        Consumer<Integer> consumerCtx = Infrastructure.decorate(consumer);
        assertThat(consumerCtx).isNotSameAs(consumer);
        assertThat(Infrastructure.decorate(consumerCtx)).isSameAs(consumerCtx);

        Predicate<Integer> predicateCtx = Infrastructure.decorate(predicate);
        assertThat(predicateCtx).isNotSameAs(predicate);
        assertThat(Infrastructure.decorate(predicateCtx)).isSameAs(predicateCtx);

        Supplier<Integer> supplierCtx = Infrastructure.decorate(supplier);
        assertThat(supplierCtx).isNotSameAs(supplier);
        assertThat(Infrastructure.decorate(supplierCtx)).isSameAs(supplierCtx);

        Function<Integer, Integer> functionCtx = Infrastructure.decorate(function);
        assertThat(functionCtx).isNotSameAs(function);
        assertThat(Infrastructure.decorate(functionCtx)).isSameAs(functionCtx);

        Functions.TriConsumer<Integer, Integer, Integer> triConsumerCtx = Infrastructure.decorate(triConsumer);
        assertThat(triConsumerCtx).isNotSameAs(triConsumer);
        assertThat(Infrastructure.decorate(triConsumerCtx)).isSameAs(triConsumerCtx);

        BinaryOperator<Integer> binaryOperatorCtx = Infrastructure.decorate(binOp);
        assertThat(binaryOperatorCtx).isNotSameAs(binOp);
        assertThat(Infrastructure.decorate(binaryOperatorCtx)).isSameAs(binaryOperatorCtx);

        LongConsumer longConsumerCtx = Infrastructure.decorate(longConsumer);
        assertThat(longConsumerCtx).isNotSameAs(longConsumer);
        assertThat(Infrastructure.decorate(longConsumerCtx)).isSameAs(longConsumerCtx);

        Functions.Function3<Integer, Integer, Integer, Integer> function3Ctx = Infrastructure.decorate(function3);
        assertThat(function3Ctx).isNotSameAs(function3);
        assertThat(Infrastructure.decorate(function3Ctx)).isSameAs(function3Ctx);

        Functions.Function4<Integer, Integer, Integer, Integer, Integer> function4Ctx = Infrastructure.decorate(function4);
        assertThat(function4Ctx).isNotSameAs(function4);
        assertThat(Infrastructure.decorate(function4Ctx)).isSameAs(function4Ctx);

        Functions.Function5<Integer, Integer, Integer, Integer, Integer, Integer> function5Ctx = Infrastructure
                .decorate(function5);
        assertThat(function5Ctx).isNotSameAs(function5);
        assertThat(Infrastructure.decorate(function5Ctx)).isSameAs(function5Ctx);

        Functions.Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer> function6Ctx = Infrastructure
                .decorate(function6);
        assertThat(function6Ctx).isNotSameAs(function6);
        assertThat(Infrastructure.decorate(function6Ctx)).isSameAs(function6Ctx);

        Functions.Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> function7Ctx = Infrastructure
                .decorate(function7);
        assertThat(function7Ctx).isNotSameAs(function7);
        assertThat(Infrastructure.decorate(function7Ctx)).isSameAs(function7Ctx);

        Functions.Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> function8Ctx = Infrastructure
                .decorate(function8);
        assertThat(function8Ctx).isNotSameAs(function8);
        assertThat(Infrastructure.decorate(function8Ctx)).isSameAs(function8Ctx);

        Functions.Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> function9Ctx = Infrastructure
                .decorate(function9);
        assertThat(function9Ctx).isNotSameAs(function9);
        assertThat(Infrastructure.decorate(function9Ctx)).isSameAs(function9Ctx);
    }

}
