package io.smallrye.reactive.adapt;

import io.smallrye.reactive.Uni;
import org.reactivestreams.Publisher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniAdaptFrom {

    @SuppressWarnings("unchecked")
    public static <O, T> Uni<T> adaptFrom(O instance) {

        nonNull(instance, "instance");

        if (instance instanceof Uni) {
            return (Uni) instance;
        }

        if (instance instanceof CompletionStage) {
            return Uni.createFrom().completionStage((CompletionStage) instance);
        }

        if (instance instanceof Publisher) {
            return Uni.createFrom().publisher((Publisher) instance);
        }

        ServiceLoader<UniAdapter> adapters = ServiceLoader.load(UniAdapter.class);
        for (UniAdapter adapter : adapters) {
            if (adapter.accept(instance.getClass())) {
                return adapter.adaptFrom(instance);
            }
        }

        Uni<T> uni = instantiateUsingToPublisher(instance);
        if (uni == null) {
            uni = instantiateUsingToFlowable(instance);
            if (uni == null) {
                throw new RuntimeException(
                        "Unable to create an instance of Uni from an instance of " + instance.getClass().getName()
                                + ", no adapter found");
            }
        }
        return uni;
    }

    private static <O> Uni instantiateUsingToPublisher(O instance) {
        try {
            Method method = instance.getClass().getMethod("toPublisher");
            Object result = method.invoke(instance);
            return Uni.createFrom().publisher((Publisher) result);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    /**
     * Very RX Java specific.
     *
     * @param instance the instance
     * @param <O>      the returned type
     * @return an instance of O or {@code null}
     */
    private static <O> Uni instantiateUsingToFlowable(O instance) {
        try {
            Method method = instance.getClass().getMethod("toFlowable");
            Object result = method.invoke(instance);
            return Uni.createFrom().publisher((Publisher) result);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }
}
