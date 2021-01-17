package io.smallrye.mutiny.infrastructure;

public interface MutinyInterceptor {

    /**
     * Default interceptor ordinal ({@code 100}).
     */
    int DEFAULT_ORDINAL = 100;

    /**
     * @return the interceptor ordinal. The ordinal is used to sort the interceptor. Lower value are executed first.
     *         Default is 100.
     */
    default int ordinal() {
        return DEFAULT_ORDINAL;
    }
}
