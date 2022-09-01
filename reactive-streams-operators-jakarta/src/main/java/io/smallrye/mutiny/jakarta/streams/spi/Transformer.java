package io.smallrye.mutiny.jakarta.streams.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

import io.smallrye.mutiny.Multi;

public class Transformer {

    private final ExecutionModel model;

    private static final Transformer INSTANCE;

    static {
        INSTANCE = new Transformer();
    }

    private Transformer() {
        ServiceLoader<ExecutionModel> loader = ServiceLoader.load(ExecutionModel.class);
        Iterator<ExecutionModel> iterator = loader.iterator();
        if (iterator.hasNext()) {
            model = iterator.next();
        } else {
            model = i -> i;
        }
    }

    /**
     * Calls the model.
     *
     * @param upstream the upstream
     * @param <T> the type of data
     * @return the decorated stream if needed
     */
    @SuppressWarnings("unchecked")
    public static <T> Multi<T> apply(Multi<T> upstream) {
        return INSTANCE.model.apply(upstream);
    }

}
