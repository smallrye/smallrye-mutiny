package io.smallrye.reactive.infrastructure;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.subscription.UniSubscriber;

public class Infrastructure {

    static {
        ServiceLoader<ExecutorConfiguration> executorLoader = ServiceLoader.load(ExecutorConfiguration.class);
        Iterator<ExecutorConfiguration> iterator = executorLoader.iterator();
        if (iterator.hasNext()) {
            ExecutorConfiguration next = iterator.next();
            DEFAULT_EXECUTOR = nonNull(next.getDefaultWorkerExecutor(), "executor");
            DEFAULT_SCHEDULER = nonNull(next.getDefaultScheduledExecutor(), "scheduler");
        } else {
            ScheduledExecutorService scheduler = Executors
                    .newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
            DEFAULT_SCHEDULER = scheduler;
            DEFAULT_EXECUTOR = scheduler;
        }

        // Interceptor
        ServiceLoader<UniInterceptor> interceptorLoader = ServiceLoader.load(UniInterceptor.class);
        List<UniInterceptor> interceptors = new ArrayList<>();
        interceptorLoader.iterator().forEachRemaining(interceptors::add);
        interceptors.sort(Comparator.comparingInt(UniInterceptor::ordinal));
        UNI_INTERCEPTORS = interceptors;
    }

    private static final ScheduledExecutorService DEFAULT_SCHEDULER;
    private static final Executor DEFAULT_EXECUTOR;
    private static final List<UniInterceptor> UNI_INTERCEPTORS;

    public static ScheduledExecutorService getDefaultWorkerPool() {
        return DEFAULT_SCHEDULER;
    }

    public static Executor getDefaultExecutor() {
        return DEFAULT_EXECUTOR;
    }

    public static <T> Uni<T> onUniCreation(Uni<T> instance) {
        Uni<T> current = instance;
        for (UniInterceptor itcp : UNI_INTERCEPTORS) {
            current = itcp.onUniCreation(current);
        }
        return current;
    }

    public static <T> UniSubscriber<? super T> onUniSubscription(Uni<T> instance, UniSubscriber<? super T> subscriber) {
        UniSubscriber<? super T> current = subscriber;
        for (UniInterceptor itcp : UNI_INTERCEPTORS) {
            current = itcp.onSubscription(instance, current);
        }
        return current;
    }

    // For testing purpose only
    static void registerUniInterceptor(UniInterceptor e) {
        UNI_INTERCEPTORS.add(e);
        UNI_INTERCEPTORS.sort(Comparator.comparingInt(UniInterceptor::ordinal));
    }

    // For testing purpose only
    static void clearUniInterceptors() {
        UNI_INTERCEPTORS.clear();
    }

    // For testing purpose only
    static List<UniInterceptor> getUniInterceptors() {
        return UNI_INTERCEPTORS;
    }

    private Infrastructure() {
        // Avoid direct instantiation.
    }
}
