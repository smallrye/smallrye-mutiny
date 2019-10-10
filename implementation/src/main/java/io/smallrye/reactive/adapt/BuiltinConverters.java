package io.smallrye.reactive.adapt;

import io.smallrye.reactive.adapt.converters.FromCompletionStage;
import io.smallrye.reactive.adapt.converters.FromFlowable;
import io.smallrye.reactive.adapt.converters.FromMaybe;
import io.smallrye.reactive.adapt.converters.FromSingle;

public class BuiltinConverters {
    private BuiltinConverters() {
        // Avoid direct instantiation
    }

    @SuppressWarnings("unchecked")
    public static <T> FromSingle<T> fromSingle() {
        return FromSingle.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> FromMaybe<T> fromMaybe() {
        return FromMaybe.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> FromFlowable<T> fromFlowable() {
        return FromFlowable.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> FromCompletionStage<T> fromCompletionStage() {
        return FromCompletionStage.INSTANCE;
    }
}
