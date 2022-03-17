package guides.operators;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.DemandPacer;
import io.smallrye.mutiny.subscription.FixedDemandPacer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ControlDemandTest {

    @Test
    void pacing() {
        // tag::pacing[]
        FixedDemandPacer pacer = new FixedDemandPacer(25L, Duration.ofMillis(100L));

        Multi<Integer> multi = Multi.createFrom().range(0, 100)
                .paceDemand().on(Infrastructure.getDefaultWorkerPool()).using(pacer);
        // end::pacing[]

        AssertSubscriber<Integer> sub = multi.subscribe().withSubscriber(AssertSubscriber.create());
        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(100);
    }

    @Test
    void customPacer() {
        // tag::custom-pacer[]
        DemandPacer pacer = new DemandPacer() {

            @Override
            public Request initial() {
                return new Request(10L, Duration.ofMillis(100L));
            }

            @Override
            public Request apply(Request previousRequest, long observedItemsCount) {
                return new Request(previousRequest.demand() * 2, previousRequest.delay().plus(10, ChronoUnit.MILLIS));
            }
        };
        // end::custom-pacer[]
    }
}
