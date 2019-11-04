package io.smallrye.reactive.converters.uni;

public class RxConverters {

    private RxConverters() {
        // Avoid direct instantiation
    }

    public static FromCompletable fromCompletable() {
        return FromCompletable.INSTANCE;
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
    public static <T> FromObservable<T> fromObservable() {
        return FromObservable.INSTANCE;
    }

}
