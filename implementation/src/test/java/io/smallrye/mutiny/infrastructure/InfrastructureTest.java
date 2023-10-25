package io.smallrye.mutiny.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    @DisplayName("Buffer sizes definitions when there are no matching system properties")
    void bufferSizesNoSysProp() {
        assertThat(Infrastructure.getBufferSizeXs()).isEqualTo(32);
        assertThat(Infrastructure.getBufferSizeS()).isEqualTo(256);

        Infrastructure.setBufferSizeXs(4);
        assertThat(Infrastructure.getBufferSizeXs()).isEqualTo(4);

        Infrastructure.setBufferSizeS(4);
        assertThat(Infrastructure.getBufferSizeS()).isEqualTo(4);

        assertThatThrownBy(() -> Infrastructure.setBufferSizeXs(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");

        assertThatThrownBy(() -> Infrastructure.setBufferSizeXs(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");

        assertThatThrownBy(() -> Infrastructure.setBufferSizeS(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");

        assertThatThrownBy(() -> Infrastructure.setBufferSizeS(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    @Test
    @DisplayName("Buffer sizes definitions when there are matching system properties")
    void bufferSizesWithSysProp() {
        try {
            System.setProperty("mutiny.buffer-size.s", "1024");
            System.setProperty("mutiny.buffer-size.xs", "64");
            assertThat(Infrastructure.getBufferSizeXs()).isEqualTo(64);
            assertThat(Infrastructure.getBufferSizeS()).isEqualTo(1024);
        } finally {
            System.clearProperty("mutiny.buffer-size.s");
            System.clearProperty("mutiny.buffer-size.xs");
        }
        assertThat(Infrastructure.getBufferSizeXs()).isEqualTo(32);
        assertThat(Infrastructure.getBufferSizeS()).isEqualTo(256);
    }
}
