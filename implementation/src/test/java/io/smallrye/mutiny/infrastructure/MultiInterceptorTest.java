package io.smallrye.mutiny.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Flow;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.test.Mocks;

public class MultiInterceptorTest {

    @Test
    public void testDefaultInterceptorBehavior() {
        MultiInterceptor interceptor = new MultiInterceptor() {
            // Default.
        };

        assertThat(interceptor.ordinal()).isEqualTo(MultiInterceptor.DEFAULT_ORDINAL);
        Multi<String> multi = new AbstractMulti<String>() {
            // Do nothing
        };
        assertThat(interceptor.onMultiCreation(multi)).isSameAs(multi);

        Flow.Subscriber<Object> subscriber = Mocks.subscriber();
        assertThat(interceptor.onSubscription(multi, subscriber)).isSameAs(subscriber);
    }

}
