package io.smallrye.mutiny.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import junit5.support.InfrastructureResource;

@ResourceLock(InfrastructureResource.NAME)
class InfrastructureTest {

    @AfterEach
    void reset() {
        Infrastructure.reload();
    }

    @Test
    @DisplayName("Ensure Infrastructure.setMultiOverflowDefaultBufferSize rejects 0 buffer sizes")
    void rejectZeroMultiOverflowBufferSizes() {
        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                () -> Infrastructure.setMultiOverflowDefaultBufferSize(0));
        assertThat(err).hasMessageContaining("must be greater than zero");
    }

    @Test
    @DisplayName("Ensure Infrastructure.setMultiOverflowDefaultBufferSize rejects negative buffer sizes")
    void rejectNegativeMultiOverflowBufferSizes() {
        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                () -> Infrastructure.setMultiOverflowDefaultBufferSize(-100));
        assertThat(err).hasMessageContaining("must be greater than zero");
    }

    @Test
    @DisplayName("Ensure Infrastructure.setMultiOverflowDefaultBufferSize accepts correct buffer sizes")
    void acceptCorrectMultiOverflowBufferSizes() {
        Infrastructure.setMultiOverflowDefaultBufferSize(256);
        assertThat(Infrastructure.getMultiOverflowDefaultBufferSize()).isEqualTo(256);
    }
}
