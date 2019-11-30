package io.smallrye.mutiny.converters.uni;

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

    @SuppressWarnings("unchecked")
    public static <T> ToMono<T> toMono() {
        return ToMono.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToFlux<T> toFlux() {
        return ToFlux.INSTANCE;
    }
}
