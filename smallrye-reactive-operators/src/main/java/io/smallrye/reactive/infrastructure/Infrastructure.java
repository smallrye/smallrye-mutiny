package io.smallrye.reactive.infrastructure;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

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
    }

    private static final  ScheduledExecutorService DEFAULT_SCHEDULER;
    private static final  Executor DEFAULT_EXECUTOR;

    public static ScheduledExecutorService getDefaultWorkerPool() {
        return DEFAULT_SCHEDULER;
    }

    public static Executor getDefaultExecutor() {
        return DEFAULT_EXECUTOR;
    }

}
