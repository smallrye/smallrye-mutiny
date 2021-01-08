package io.smallrye.mutiny.infrastructure;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.UniSubscriber;

public class Infrastructure {

    static {
        ServiceLoader<ExecutorConfiguration> executorLoader = ServiceLoader.load(ExecutorConfiguration.class);
        Iterator<ExecutorConfiguration> iterator = executorLoader.iterator();
        if (iterator.hasNext()) {
            ExecutorConfiguration next = iterator.next();
            setDefaultExecutor(nonNull(next.getDefaultWorkerExecutor(), "executor"));
        } else {
            setDefaultExecutor();
        }

        // Interceptor
        ServiceLoader<UniInterceptor> uniItcp = ServiceLoader.load(UniInterceptor.class);
        List<UniInterceptor> interceptors = new ArrayList<>();
        uniItcp.iterator().forEachRemaining(interceptors::add);
        interceptors.sort(Comparator.comparingInt(UniInterceptor::ordinal));
        UNI_INTERCEPTORS = interceptors;

        ServiceLoader<MultiInterceptor> multiItcp = ServiceLoader.load(MultiInterceptor.class);
        List<MultiInterceptor> interceptors2 = new ArrayList<>();
        multiItcp.iterator().forEachRemaining(interceptors2::add);
        interceptors2.sort(Comparator.comparingInt(MultiInterceptor::ordinal));
        MULTI_INTERCEPTORS = interceptors2;

        resetCanCallerThreadBeBlockedSupplier();
    }

    /**
     * Configure or reset the executors.
     */
    public static void setDefaultExecutor() {
        ExecutorService scheduler = ForkJoinPool.commonPool();
        setDefaultExecutor(scheduler);
    }

    private static ScheduledExecutorService DEFAULT_SCHEDULER;
    private static Executor DEFAULT_EXECUTOR;
    private static final List<UniInterceptor> UNI_INTERCEPTORS;
    private static final List<MultiInterceptor> MULTI_INTERCEPTORS;
    private static UnaryOperator<CompletableFuture<?>> completableFutureWrapper;
    private static Consumer<Throwable> droppedExceptionHandler = Infrastructure::printAndDump;
    private static BooleanSupplier canCallerThreadBeBlockedSupplier;

    public static void setDefaultExecutor(Executor s) {
        if (s == DEFAULT_EXECUTOR) {
            return;
        }
        Executor existing = DEFAULT_EXECUTOR;
        if (existing instanceof ExecutorService) {
            ((ExecutorService) existing).shutdownNow();
        }
        DEFAULT_EXECUTOR = s;
        DEFAULT_SCHEDULER = new MutinyScheduler(s);
    }

    public static ScheduledExecutorService getDefaultWorkerPool() {
        return DEFAULT_SCHEDULER;
    }

    public static Executor getDefaultExecutor() {
        return DEFAULT_EXECUTOR;
    }

    public static <T> Uni<T> onUniCreation(Uni<T> instance) {
        if (UNI_INTERCEPTORS.isEmpty()) {
            return instance;
        }
        Uni<T> current = instance;
        for (UniInterceptor itcp : UNI_INTERCEPTORS) {
            current = itcp.onUniCreation(current);
        }
        return current;
    }

    public static <T> Multi<T> onMultiCreation(Multi<T> instance) {
        if (MULTI_INTERCEPTORS.isEmpty()) {
            return instance;
        }
        Multi<T> current = instance;
        for (MultiInterceptor interceptor : MULTI_INTERCEPTORS) {
            current = interceptor.onMultiCreation(current);
        }
        return current;
    }

    public static <T> UniSubscriber<? super T> onUniSubscription(Uni<T> instance, UniSubscriber<? super T> subscriber) {
        if (UNI_INTERCEPTORS.isEmpty()) {
            return subscriber;
        }
        UniSubscriber<? super T> current = subscriber;
        for (UniInterceptor interceptor : UNI_INTERCEPTORS) {
            current = interceptor.onSubscription(instance, current);
        }
        return current;
    }

    public static <T> Subscriber<? super T> onMultiSubscription(Publisher<? extends T> instance,
            Subscriber<? super T> subscriber) {
        if (MULTI_INTERCEPTORS.isEmpty()) {
            return subscriber;
        }
        Subscriber<? super T> current = subscriber;
        for (MultiInterceptor itcp : MULTI_INTERCEPTORS) {
            current = itcp.onSubscription(instance, current);
        }
        return current;
    }

    static List<UniInterceptor> getUniInterceptors() {
        return UNI_INTERCEPTORS;
    }

    public static void setCompletableFutureWrapper(UnaryOperator<CompletableFuture<?>> wrapper) {
        completableFutureWrapper = wrapper;
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> wrapCompletableFuture(CompletableFuture<T> future) {
        UnaryOperator<CompletableFuture<?>> wrapper = completableFutureWrapper;
        return wrapper != null ? (CompletableFuture<T>) wrapper.apply(future) : future;
    }

    public static void handleDroppedException(Throwable throwable) {
        droppedExceptionHandler.accept(throwable);
    }

    /**
     * Defines a custom caller thread blocking check supplier.
     *
     * @param supplier the supplier, must not be {@code null} and must not throw an exception or it will also be lost.
     */
    public static void setCanCallerThreadBeBlockedSupplier(BooleanSupplier supplier) {
        nonNull(supplier, "supplier");
        canCallerThreadBeBlockedSupplier = supplier;
    }

    public static boolean canCallerThreadBeBlocked() {
        return canCallerThreadBeBlockedSupplier.getAsBoolean();
    }

    /**
     * Defines a custom dropped exception handler.
     * 
     * @param handler the handler, must not be {@code null} and must not throw an exception or it will also be lost.
     */
    public static void setDroppedExceptionHandler(Consumer<Throwable> handler) {
        ParameterValidation.nonNull(handler, "handler");
        droppedExceptionHandler = handler;
    }

    private static void printAndDump(Throwable throwable) {
        System.err.println("[-- Mutiny had to drop the following exception --]");
        StackTraceElement element = Thread.currentThread().getStackTrace()[3];
        System.err.println("Exception received by: " + element.toString());
        throwable.printStackTrace();
        System.err.println("[------------------------------------------------]");
    }

    // For testing purpose only
    static void registerUniInterceptor(UniInterceptor e) {
        UNI_INTERCEPTORS.add(e);
        UNI_INTERCEPTORS.sort(Comparator.comparingInt(UniInterceptor::ordinal));
    }

    // For testing purpose only
    public static void reloadUniInterceptors() {
        ServiceLoader<UniInterceptor> interceptorLoader = ServiceLoader.load(UniInterceptor.class);
        List<UniInterceptor> interceptors = new ArrayList<>();
        interceptorLoader.iterator().forEachRemaining(interceptors::add);
        interceptors.sort(Comparator.comparingInt(UniInterceptor::ordinal));
        UNI_INTERCEPTORS.addAll(interceptors);
    }

    // For testing purpose only
    public static void reloadMultiInterceptors() {
        ServiceLoader<MultiInterceptor> interceptorLoader = ServiceLoader.load(MultiInterceptor.class);
        List<MultiInterceptor> interceptors = new ArrayList<>();
        interceptorLoader.iterator().forEachRemaining(interceptors::add);
        interceptors.sort(Comparator.comparingInt(MultiInterceptor::ordinal));
        MULTI_INTERCEPTORS.addAll(interceptors);
    }

    // For testing purpose only
    public static void clearInterceptors() {
        UNI_INTERCEPTORS.clear();
        MULTI_INTERCEPTORS.clear();
    }

    // For testing purpose only
    public static void resetDroppedExceptionHandler() {
        droppedExceptionHandler = Infrastructure::printAndDump;
    }

    // For testing purpose only
    public static void resetCanCallerThreadBeBlockedSupplier() {
        canCallerThreadBeBlockedSupplier = () -> true;
    }

    private Infrastructure() {
        // Avoid direct instantiation.
    }
}
