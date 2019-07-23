package io.smallrye.reactive.adapt;

import io.smallrye.reactive.Uni;
import org.reactivestreams.Publisher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniAdaptTo<O> {

    private final Class<O> output;
    private final Uni<?> uni;
    private final ServiceLoader<UniAdapter> adapters;

    public UniAdaptTo(Uni<?> uni, Class<O> output) {
        this.uni = nonNull(uni, "uni");
        this.output = nonNull(output, "output");
        this.adapters = ServiceLoader.load(UniAdapter.class);
    }

    @SuppressWarnings("unchecked")
    public O adapt() {

        if (output.isInstance(uni)) {
            return (O) uni;
        }

        if (output.isAssignableFrom(CompletableFuture.class)) {
            return (O) uni.subscribe().asCompletionStage();
        }

        for (UniAdapter adapter : this.adapters) {
            if (adapter.accept(output)) {
                return ((UniAdapter<O>) adapter).adaptTo(uni);
            }
        }

        O instance = instantiateUsingFromPublisher();
        if (instance == null) {
            instance = instantiateUsingFrom();
            if (instance == null) {
                throw new IllegalArgumentException("Unable to create an instance of " + output.getName() + " from a Uni, no adapter found");
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private O instantiateUsingFromPublisher() {
        try {
            Method method = output.getMethod("fromPublisher", Publisher.class);
            if (Modifier.isStatic(method.getModifiers())) {
                return (O) method.invoke(null, uni.adapt().toPublisher());
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private O instantiateUsingFrom() {
        try {
            Method method = output.getMethod("from", Publisher.class);
            if (Modifier.isStatic(method.getModifiers())) {
                return (O) method.invoke(null, uni.adapt().toPublisher());
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
        return null;
    }

}
