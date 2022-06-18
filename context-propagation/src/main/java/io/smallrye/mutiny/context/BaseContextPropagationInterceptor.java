package io.smallrye.mutiny.context;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.*;

import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.impl.Contextualized;
import io.smallrye.mutiny.infrastructure.CallbackDecorator;
import io.smallrye.mutiny.tuples.Functions;

public abstract class BaseContextPropagationInterceptor implements CallbackDecorator {

    /**
     * Gets the Context Propagation ThreadContext. External
     * implementations may implement this method.
     *
     * @return the ThreadContext
     */
    protected abstract SmallRyeThreadContext getThreadContext();

    @Override
    public <T> Supplier<T> decorate(Supplier<T> supplier) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(supplier)) {
            return supplier;
        }
        return context.contextualSupplier(supplier);
    }

    @Override
    public <T> Consumer<T> decorate(Consumer<T> consumer) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(consumer)) {
            return consumer;
        }
        return context.contextualConsumer(consumer);
    }

    @Override
    public LongConsumer decorate(LongConsumer consumer) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(consumer) || context.isEmpty()) {
            return consumer;
        }
        Consumer<Long> cons = context.contextualConsumer(consumer::accept);
        return new ContextualizedLongConsumer(cons);
    }

    static class ContextualizedLongConsumer implements LongConsumer, Contextualized {
        private final Consumer<Long> contextualized;

        ContextualizedLongConsumer(Consumer<Long> contextualized) {
            this.contextualized = contextualized;
        }

        @Override
        public void accept(long value) {
            this.contextualized.accept(value);
        }
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(runnable)) {
            return runnable;
        }
        return context.contextualRunnable(runnable);
    }

    @Override
    public <V> Callable<V> decorate(Callable<V> callable) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(callable)) {
            return callable;
        }
        return context.contextualCallable(callable);
    }

    @Override
    public <T1, T2> BiConsumer<T1, T2> decorate(BiConsumer<T1, T2> consumer) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(consumer)) {
            return consumer;
        }
        return context.contextualConsumer(consumer);
    }

    @Override
    public <I, O> Function<I, O> decorate(Function<I, O> function) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(function)) {
            return function;
        }
        return context.contextualFunction(function);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I1, I2, I3, O> Functions.Function3<I1, I2, I3, O> decorate(Functions.Function3<I1, I2, I3, O> function) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(function) || context.isEmpty()) {
            return function;
        }
        Function<Object[], O> fun = context
                .contextualFunction(args -> function.apply((I1) args[0], (I2) args[1], (I3) args[2]));
        return new ContextualizedFunction3<>(fun);
    }

    static class ContextualizedFunction3<I1, I2, I3, O> implements Functions.Function3<I1, I2, I3, O>, Contextualized {

        private final Function<Object[], O> function;

        ContextualizedFunction3(Function<Object[], O> function) {
            this.function = function;
        }

        @Override
        public O apply(I1 item1, I2 item2, I3 item3) {
            return function.apply(new Object[] { item1, item2, item3 });
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I1, I2, I3, I4, O> Functions.Function4<I1, I2, I3, I4, O> decorate(
            Functions.Function4<I1, I2, I3, I4, O> function) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(function) || context.isEmpty()) {
            return function;
        }
        Function<Object[], O> fun = context
                .contextualFunction(args -> function.apply((I1) args[0], (I2) args[1], (I3) args[2], (I4) args[3]));
        return new ContextualizedFunction4<>(fun);
    }

    static class ContextualizedFunction4<I1, I2, I3, I4, O> implements Functions.Function4<I1, I2, I3, I4, O>, Contextualized {

        private final Function<Object[], O> function;

        ContextualizedFunction4(Function<Object[], O> function) {
            this.function = function;
        }

        @Override
        public O apply(I1 item1, I2 item2, I3 item3, I4 item4) {
            return function.apply(new Object[] { item1, item2, item3, item4 });
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I1, I2, I3, I4, I5, O> Functions.Function5<I1, I2, I3, I4, I5, O> decorate(
            Functions.Function5<I1, I2, I3, I4, I5, O> function) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(function) || context.isEmpty()) {
            return function;
        }
        Function<Object[], O> fun = context
                .contextualFunction(
                        args -> function.apply((I1) args[0], (I2) args[1], (I3) args[2], (I4) args[3], (I5) args[4]));
        return new ContextualizedFunction5<>(fun);
    }

    static class ContextualizedFunction5<I1, I2, I3, I4, I5, O>
            implements Functions.Function5<I1, I2, I3, I4, I5, O>, Contextualized {

        private final Function<Object[], O> function;

        ContextualizedFunction5(Function<Object[], O> function) {
            this.function = function;
        }

        @Override
        public O apply(I1 item1, I2 item2, I3 item3, I4 item4, I5 item5) {
            return function.apply(new Object[] { item1, item2, item3, item4, item5 });
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I1, I2, I3, I4, I5, I6, O> Functions.Function6<I1, I2, I3, I4, I5, I6, O> decorate(
            Functions.Function6<I1, I2, I3, I4, I5, I6, O> function) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(function) || context.isEmpty()) {
            return function;
        }
        Function<Object[], O> fun = context
                .contextualFunction(args -> function.apply((I1) args[0], (I2) args[1], (I3) args[2], (I4) args[3], (I5) args[4],
                        (I6) args[5]));
        return new ContextualizedFunction6<>(fun);

    }

    static class ContextualizedFunction6<I1, I2, I3, I4, I5, I6, O>
            implements Functions.Function6<I1, I2, I3, I4, I5, I6, O>, Contextualized {

        private final Function<Object[], O> function;

        ContextualizedFunction6(Function<Object[], O> function) {
            this.function = function;
        }

        @Override
        public O apply(I1 item1, I2 item2, I3 item3, I4 item4, I5 item5, I6 item6) {
            return function.apply(new Object[] { item1, item2, item3, item4, item5, item6 });
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I1, I2, I3, I4, I5, I6, I7, O> Functions.Function7<I1, I2, I3, I4, I5, I6, I7, O> decorate(
            Functions.Function7<I1, I2, I3, I4, I5, I6, I7, O> function) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(function) || context.isEmpty()) {
            return function;
        }
        Function<Object[], O> fun = context
                .contextualFunction(args -> function.apply((I1) args[0], (I2) args[1], (I3) args[2], (I4) args[3], (I5) args[4],
                        (I6) args[5], (I7) args[6]));
        return new ContextualizedFunction7<>(fun);

    }

    static class ContextualizedFunction7<I1, I2, I3, I4, I5, I6, I7, O>
            implements Functions.Function7<I1, I2, I3, I4, I5, I6, I7, O>, Contextualized {

        private final Function<Object[], O> function;

        ContextualizedFunction7(Function<Object[], O> function) {
            this.function = function;
        }

        @Override
        public O apply(I1 item1, I2 item2, I3 item3, I4 item4, I5 item5, I6 item6, I7 item7) {
            return function.apply(new Object[] { item1, item2, item3, item4, item5, item6, item7 });
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I1, I2, I3, I4, I5, I6, I7, I8, O> Functions.Function8<I1, I2, I3, I4, I5, I6, I7, I8, O> decorate(
            Functions.Function8<I1, I2, I3, I4, I5, I6, I7, I8, O> function) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(function) || context.isEmpty()) {
            return function;
        }
        Function<Object[], O> fun = context
                .contextualFunction(args -> function.apply((I1) args[0], (I2) args[1], (I3) args[2], (I4) args[3], (I5) args[4],
                        (I6) args[5], (I7) args[6], (I8) args[7]));
        return new ContextualizedFunction8<>(fun);

    }

    static class ContextualizedFunction8<I1, I2, I3, I4, I5, I6, I7, I8, O>
            implements Functions.Function8<I1, I2, I3, I4, I5, I6, I7, I8, O>, Contextualized {

        private final Function<Object[], O> function;

        ContextualizedFunction8(Function<Object[], O> function) {
            this.function = function;
        }

        @Override
        public O apply(I1 item1, I2 item2, I3 item3, I4 item4, I5 item5, I6 item6, I7 item7, I8 item8) {
            return function.apply(new Object[] { item1, item2, item3, item4, item5, item6, item7, item8 });
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I1, I2, I3, I4, I5, I6, I7, I8, I9, O> Functions.Function9<I1, I2, I3, I4, I5, I6, I7, I8, I9, O> decorate(
            Functions.Function9<I1, I2, I3, I4, I5, I6, I7, I8, I9, O> function) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(function) || context.isEmpty()) {
            return function;
        }
        Function<Object[], O> fun = context
                .contextualFunction(args -> function.apply((I1) args[0], (I2) args[1], (I3) args[2], (I4) args[3], (I5) args[4],
                        (I6) args[5], (I7) args[6], (I8) args[7], (I9) args[8]));
        return new ContextualizedFunction9<>(fun);
    }

    static class ContextualizedFunction9<I1, I2, I3, I4, I5, I6, I7, I8, I9, O>
            implements Functions.Function9<I1, I2, I3, I4, I5, I6, I7, I8, I9, O>, Contextualized {

        private final Function<Object[], O> function;

        ContextualizedFunction9(Function<Object[], O> function) {
            this.function = function;
        }

        @Override
        public O apply(I1 item1, I2 item2, I3 item3, I4 item4, I5 item5, I6 item6, I7 item7, I8 item8, I9 item9) {
            return function.apply(new Object[] { item1, item2, item3, item4, item5, item6, item7, item8, item9 });
        }
    }

    @Override
    public <I1, I2, O> BiFunction<I1, I2, O> decorate(BiFunction<I1, I2, O> function) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(function)) {
            return function;
        }
        return context.contextualFunction(function);
    }

    @Override
    public <T> BinaryOperator<T> decorate(BinaryOperator<T> operator) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(operator) || context.isEmpty()) {
            return operator;
        }
        BiFunction<T, T, T> function = context.contextualFunction(operator);
        return new ContextualizedBinaryOperator<>(function);
    }

    static class ContextualizedBinaryOperator<T> implements BinaryOperator<T>, Contextualized {
        private final BiFunction<T, T, T> contextualized;

        ContextualizedBinaryOperator(BiFunction<T, T, T> contextualized) {
            this.contextualized = contextualized;
        }

        @Override
        public T apply(T t, T t2) {
            return contextualized.apply(t, t2);
        }
    }

    @Override
    public <T1, T2, T3> Functions.TriConsumer<T1, T2, T3> decorate(Functions.TriConsumer<T1, T2, T3> consumer) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(consumer) || context.isEmpty()) {
            return consumer;
        }
        return new ContextualizedTriConsumer<>(context.currentContextExecutor(), consumer);
    }

    static class ContextualizedTriConsumer<T1, T2, T3> implements Functions.TriConsumer<T1, T2, T3>, Contextualized {

        private final Executor executor;
        private final Functions.TriConsumer<T1, T2, T3> consumer;

        ContextualizedTriConsumer(Executor executor, Functions.TriConsumer<T1, T2, T3> consumer) {
            this.executor = executor;
            this.consumer = consumer;
        }

        @Override
        public void accept(T1 t1, T2 t2, T3 t3) {
            executor.execute(() -> consumer.accept(t1, t2, t3));
        }
    }

    @Override
    public BooleanSupplier decorate(BooleanSupplier supplier) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(supplier) || context.isEmpty()) {
            return supplier;
        }
        Supplier<Boolean> contextualized = context.contextualSupplier(supplier::getAsBoolean);
        return new ContextualizedBooleanSupplier(contextualized);
    }

    static class ContextualizedBooleanSupplier implements BooleanSupplier, Contextualized {
        private final Supplier<Boolean> contextualized;

        ContextualizedBooleanSupplier(Supplier<Boolean> contextualized) {
            this.contextualized = contextualized;
        }

        @Override
        public boolean getAsBoolean() {
            return contextualized.get();
        }
    }

    @Override
    public <T> Predicate<T> decorate(Predicate<T> predicate) {
        SmallRyeThreadContext context = getThreadContext();
        if (context.isContextualized(predicate) || context.isEmpty()) {
            return predicate;
        }
        Function<T, Boolean> contextualized = context.contextualFunction(predicate::test);
        return new ContextualizedPredicate<>(contextualized);
    }

    static class ContextualizedPredicate<T> implements Predicate<T>, Contextualized {
        private final Function<T, Boolean> contextualized;

        ContextualizedPredicate(Function<T, Boolean> contextualized) {
            this.contextualized = contextualized;
        }

        @Override
        public boolean test(T t) {
            return contextualized.apply(t);
        }
    }
}
