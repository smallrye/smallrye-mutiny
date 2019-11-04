package io.smallrye.reactive.converters.multi;

public class ReactorConverters {
    private ReactorConverters() {
        // Avoid direct instantiation
    }

    @SuppressWarnings("unchecked")
    public static <T> FromMono<T> fromMono() {
        return FromMono.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> FromFlux<T> fromFlux() {
        return FromFlux.INSTANCE;
    }

}
