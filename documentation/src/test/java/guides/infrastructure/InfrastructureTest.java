package guides.infrastructure;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.infrastructure.MutinyScheduler;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

public class InfrastructureTest {

    @Test
    public void test() {
        // <infra>
        Uni<Integer> uni1 = Uni.createFrom().item(1)
                .emitOn(Infrastructure.getDefaultExecutor());

        Uni<Integer> uni2 = Uni.createFrom().item(2)
                .onItem().delayIt()
                    .onExecutor(Infrastructure.getDefaultWorkerPool())
                    .by(Duration.ofMillis(10));
        // </infra>

        assertThat(uni1.await().indefinitely()).isEqualTo(1);
        assertThat(uni2.await().indefinitely()).isEqualTo(2);


        Executor executor = Runnable::run;
        // <set-infra>
        Infrastructure.setDefaultExecutor(executor);
        // </set-infra>

        assertThat(Infrastructure.getDefaultExecutor()).isEqualTo(executor);
        assertThat(Infrastructure.getDefaultWorkerPool()).isInstanceOf(MutinyScheduler.class);
    }
}
