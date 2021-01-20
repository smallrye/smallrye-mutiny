package io.smallrye.mutiny.infrastructure;

import java.lang.reflect.Field;
import java.util.*;

public class InfrastructureHelper {

    private static final Field uni_interceptors;

    private static final Field multi_interceptors;

    private static final Field callback_decorators;

    static {
        try {
            uni_interceptors = Infrastructure.class.getDeclaredField("UNI_INTERCEPTORS");
            multi_interceptors = Infrastructure.class.getDeclaredField("MULTI_INTERCEPTORS");
            callback_decorators = Infrastructure.class.getDeclaredField("CALLBACK_DECORATORS");

            uni_interceptors.setAccessible(true);
            multi_interceptors.setAccessible(true);
            callback_decorators.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }

    }

    public static void registerUniInterceptor(UniInterceptor itcp) {
        try {
            UniInterceptor[] array = (UniInterceptor[]) uni_interceptors.get(null);
            List<UniInterceptor> list = new ArrayList<>(Arrays.asList(array));
            list.add(itcp);
            list.sort(Comparator.comparingInt(MutinyInterceptor::ordinal));
            uni_interceptors.set(null, list.toArray(new UniInterceptor[0]));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void registerCallbackDecorator(CallbackDecorator cd) {
        try {
            CallbackDecorator[] array = (CallbackDecorator[]) callback_decorators.get(null);
            List<CallbackDecorator> list = new ArrayList<>(Arrays.asList(array));
            list.add(cd);
            list.sort(Comparator.comparingInt(MutinyInterceptor::ordinal));
            callback_decorators.set(null, list.toArray(new CallbackDecorator[0]));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<UniInterceptor> getUniInterceptors() {
        try {
            UniInterceptor[] itcp = (UniInterceptor[]) uni_interceptors.get(null);
            return Arrays.asList(itcp);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
