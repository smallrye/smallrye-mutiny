package io.smallrye.reactive.helpers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Infrastructure {


    private static ScheduledExecutorService DEFAULT_EXECUTOR;

    public static synchronized void configureDefaultExecutor(ScheduledExecutorService executor) {
        DEFAULT_EXECUTOR = executor;
    }


    public static synchronized ScheduledExecutorService getDefaultExecutor() {
        if (DEFAULT_EXECUTOR == null) {
            // TODO
            configureDefaultExecutor(Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors()));
        }
        return DEFAULT_EXECUTOR;
    }
}
