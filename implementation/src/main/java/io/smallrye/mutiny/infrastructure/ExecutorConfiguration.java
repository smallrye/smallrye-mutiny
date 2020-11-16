package io.smallrye.mutiny.infrastructure;

import java.util.concurrent.Executor;

/**
 * SPI allowing customizing the default executor.
 * Implementors must register their implementation by indicating the fully qualified name of the implementation in the
 * {@code META-INF/services/io.smallrye.infrastructure.ExecutorConfiguration} file.
 * <p>
 * The SPI implementation is responsible for creating and terminating the created thread pools.
 */
public interface ExecutorConfiguration {

    /**
     * Gets the default executor.
     *
     * @return the default executor.
     */
    Executor getDefaultWorkerExecutor();

}
