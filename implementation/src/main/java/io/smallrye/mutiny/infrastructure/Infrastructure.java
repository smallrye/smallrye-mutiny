package io.smallrye.mutiny.infrastructure;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.UnaryOperator;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;

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
    }

    private static final ScheduledExecutorService DEFAULT_SCHEDULER;
    private static final Executor DEFAULT_EXECUTOR;
    private static final List<UniInterceptor> UNI_INTERCEPTORS;
    private static final List<MultiInterceptor> MULTI_INTERCEPTORS;
    private static UnaryOperator<CompletableFuture<?>> completableFutureWrapper;

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

    public static <T> Multi<T> onMultiCreation(Multi<T> instance) {
        Multi<T> current = instance;
        for (MultiInterceptor interceptor : MULTI_INTERCEPTORS) {
            current = interceptor.onMultiCreation(current);
        }
        return current;
    }

    public static <T> UniSubscriber<? super T> onUniSubscription(Uni<T> instance, UniSubscriber<? super T> subscriber) {
        UniSubscriber<? super T> current = subscriber;
        for (UniInterceptor interceptor : UNI_INTERCEPTORS) {
            current = interceptor.onSubscription(instance, current);
        }
        return current;
    }

    public static <T> Subscriber<? super T> onMultiSubscription(Publisher<? extends T> instance,
            Subscriber<? super T> subscriber) {
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

    private Infrastructure() {
        // Avoid direct instantiation.
    }
}
