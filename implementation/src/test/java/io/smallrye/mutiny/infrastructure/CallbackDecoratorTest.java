package io.smallrye.mutiny.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.function.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.tuples.Functions;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
public class CallbackDecoratorTest {

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

    Functions.Function3<Integer, Integer, Integer, Integer> fn3 = (a, b, c) -> 0;
    Functions.Function4<Integer, Integer, Integer, Integer, Integer> fn4 = (a, b, c, d) -> 0;
    Functions.Function5<Integer, Integer, Integer, Integer, Integer, Integer> fn5 = (a, b, c, d, e) -> 0;
    Functions.Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer> fn6 = (a, b, c, d, e, f) -> 0;
    Functions.Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> fn7 = (a, b, c, d, e, f,
            g) -> 0;
    Functions.Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> fn8 = (a, b, c, d, e,
            f, g, h) -> 0;
    Functions.Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> fn9 = (a, b,
            c, d, e, f, g, h, i) -> 0;

    @AfterEach
    public void cleanup() {
        Infrastructure.clearInterceptors();
    }

    @Test
    public void testWithoutDecorator() {
        assertThat(Infrastructure.decorate(runnable)).isSameAs(runnable);
        assertThat(Infrastructure.decorate(biConsumer)).isSameAs(biConsumer);
        assertThat(Infrastructure.decorate(biFunction)).isSameAs(biFunction);
        assertThat(Infrastructure.decorate(booleanSupplier)).isSameAs(booleanSupplier);
        assertThat(Infrastructure.decorate(consumer)).isSameAs(consumer);
        assertThat(Infrastructure.decorate(predicate)).isSameAs(predicate);
        assertThat(Infrastructure.decorate(supplier)).isSameAs(supplier);
        assertThat(Infrastructure.decorate(function)).isSameAs(function);

        assertThat(Infrastructure.decorate(triConsumer)).isSameAs(triConsumer);
        assertThat(Infrastructure.decorate(binOp)).isSameAs(binOp);
        assertThat(Infrastructure.decorate(longConsumer)).isSameAs(longConsumer);

        assertThat(Infrastructure.decorate(fn3)).isSameAs(fn3);
        assertThat(Infrastructure.decorate(fn4)).isSameAs(fn4);
        assertThat(Infrastructure.decorate(fn5)).isSameAs(fn5);
        assertThat(Infrastructure.decorate(fn6)).isSameAs(fn6);
        assertThat(Infrastructure.decorate(fn7)).isSameAs(fn7);
        assertThat(Infrastructure.decorate(fn8)).isSameAs(fn8);
        assertThat(Infrastructure.decorate(fn9)).isSameAs(fn9);
    }

    @Test
    public void testDefaultCallbackDecorator() {

        CallbackDecorator decorator = new CallbackDecorator() {
            // Keep all the defaults
        };

        assertThat(decorator.decorate(runnable)).isSameAs(runnable);
        assertThat(decorator.decorate(biConsumer)).isSameAs(biConsumer);
        assertThat(decorator.decorate(biFunction)).isSameAs(biFunction);
        assertThat(decorator.decorate(booleanSupplier)).isSameAs(booleanSupplier);
        assertThat(decorator.decorate(consumer)).isSameAs(consumer);
        assertThat(decorator.decorate(predicate)).isSameAs(predicate);
        assertThat(decorator.decorate(supplier)).isSameAs(supplier);
        assertThat(decorator.decorate(function)).isSameAs(function);

        assertThat(decorator.decorate(triConsumer)).isSameAs(triConsumer);
        assertThat(decorator.decorate(binOp)).isSameAs(binOp);
        assertThat(decorator.decorate(longConsumer)).isSameAs(longConsumer);

        assertThat(decorator.decorate(fn3)).isSameAs(fn3);
        assertThat(decorator.decorate(fn4)).isSameAs(fn4);
        assertThat(decorator.decorate(fn5)).isSameAs(fn5);
        assertThat(decorator.decorate(fn6)).isSameAs(fn6);
        assertThat(decorator.decorate(fn7)).isSameAs(fn7);
        assertThat(decorator.decorate(fn8)).isSameAs(fn8);
        assertThat(decorator.decorate(fn9)).isSameAs(fn9);
    }

    @Test
    public void testWithOneDecorator() {
        CallbackDecorator decorator = new CallbackDecorator() {
            @Override
            public <T> Supplier<T> decorate(Supplier<T> supplier) {
                return () -> null;
            }

            @Override
            public <I, O> Function<I, O> decorate(Function<I, O> function) {
                return i -> null;
            }

            @Override
            public <I1, I2, I3, I4, O> Functions.Function4<I1, I2, I3, I4, O> decorate(
                    Functions.Function4<I1, I2, I3, I4, O> function) {
                return (a, b, c, d) -> null;
            }
        };

        InfrastructureHelper.registerCallbackDecorator(decorator);

        assertThat(decorator.decorate(runnable)).isSameAs(runnable);
        assertThat(decorator.decorate(biConsumer)).isSameAs(biConsumer);
        assertThat(decorator.decorate(biFunction)).isSameAs(biFunction);
        assertThat(decorator.decorate(booleanSupplier)).isSameAs(booleanSupplier);
        assertThat(decorator.decorate(consumer)).isSameAs(consumer);
        assertThat(decorator.decorate(predicate)).isSameAs(predicate);
        assertThat(decorator.decorate(triConsumer)).isSameAs(triConsumer);
        assertThat(decorator.decorate(binOp)).isSameAs(binOp);
        assertThat(decorator.decorate(longConsumer)).isSameAs(longConsumer);
        assertThat(decorator.decorate(fn3)).isSameAs(fn3);
        assertThat(decorator.decorate(fn5)).isSameAs(fn5);
        assertThat(decorator.decorate(fn6)).isSameAs(fn6);
        assertThat(decorator.decorate(fn7)).isSameAs(fn7);
        assertThat(decorator.decorate(fn8)).isSameAs(fn8);
        assertThat(decorator.decorate(fn9)).isSameAs(fn9);

        assertThat(decorator.decorate(supplier)).isNotSameAs(supplier);
        assertThat(decorator.decorate(function)).isNotSameAs(function);
        assertThat(decorator.decorate(fn4)).isNotSameAs(fn4);

        assertThat(decorator.decorate(supplier).get()).isNull();
        assertThat(decorator.decorate(function).apply(2)).isNull();
        assertThat(decorator.decorate(fn4).apply(1, 2, 3, 4)).isNull();
    }

    @Test
    public void testOrdering() {
        Runnable another = () -> {
        };

        CallbackDecorator decorator1 = new CallbackDecorator() {
            // Keep all the defaults

            @Override
            public int ordinal() {
                return 200;
            }
        };

        CallbackDecorator decorator2 = new CallbackDecorator() {

            @Override
            public Runnable decorate(Runnable runnable) {
                if (runnable == null) {
                    fail("Didn't expected null");
                }
                return another;
            }

            @Override
            public int ordinal() {
                return 50;
            }

        };

        InfrastructureHelper.registerCallbackDecorator(decorator1);
        InfrastructureHelper.registerCallbackDecorator(decorator2);

        assertThat(Infrastructure.decorate(runnable)).isSameAs(another).isNotSameAs(runnable);
    }
}
