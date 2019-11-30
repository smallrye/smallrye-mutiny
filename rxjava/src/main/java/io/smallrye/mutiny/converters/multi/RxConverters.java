package io.smallrye.mutiny.converters.multi;

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

    @SuppressWarnings("unchecked")
    public static <T> ToSingle<T> toSingle() {
        return ToSingle.INSTANCE;
    }

    public static <T> ToSingleFailOnNull<T> toSingleOnEmptyThrow(Class<? extends Throwable> exceptionClass) {
        return new ToSingleFailOnNull<>(exceptionClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> ToMaybe<T> toMaybe() {
        return ToMaybe.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToFlowable<T> toFlowable() {
        return ToFlowable.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> ToObservable<T> toObservable() {
        return ToObservable.INSTANCE;
    }

}
