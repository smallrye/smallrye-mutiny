package io.smallrye.mutiny.subscription;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class UniSubscriptionTest {
    @Test
    public void testDefaultRequestMethod() {
        UniSubscription subscription = () -> {
            // do nothing
        };

        subscription.request(12);
        assertThatThrownBy(() -> subscription.request(-23)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> subscription.request(0)).isInstanceOf(IllegalArgumentException.class);
        subscription.request(1);
    }
}
