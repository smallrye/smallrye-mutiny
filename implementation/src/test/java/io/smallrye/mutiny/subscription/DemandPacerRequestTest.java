package io.smallrye.mutiny.subscription;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DemandPacerRequestTest {

    @Test
    void validateCorrectDemand() {
        new DemandPacer.Request(123L, Duration.ofSeconds(10));
    }

    @ParameterizedTest
    @ValueSource(longs = { -1L, 0L })
    void rejectBadDemands(long demand) {
        assertThatThrownBy(() -> new DemandPacer.Request(demand, Duration.ofSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("`demand` must be greater than zero`");
    }

    @Test
    void rejectNullDuration() {
        assertThatThrownBy(() -> new DemandPacer.Request(10L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("`delay` must not be `null`");
    }

    @Test
    void rejectNegativeDuration() {
        assertThatThrownBy(() -> new DemandPacer.Request(10L, Duration.ofSeconds(-10L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("`delay` must be greater than zero");
    }

    @Test
    void rejectDurationOfForever() {
        assertThatThrownBy(() -> new DemandPacer.Request(100L, ChronoUnit.FOREVER.getDuration()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ChronoUnit.FOREVER is not a correct delay value");
    }
}
