package snippets;

import static org.assertj.core.api.Assertions.assertThat;

import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class UniBlockingTest {

    @Test
    public void test() {
        // tag::code[]
        Uni<String> blocking = Uni.createFrom().item(this::invokeRemoteServiceUsingBlockingIO)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        // end::code[]
        assertThat(blocking.await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void testEmitOn() {
        // tag::code-emitOn[]
        Multi<String> multi = Multi.createFrom().items("john", "jack", "sue")
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transform(this::invokeRemoteServiceUsingBlockingIO);
        // end::code-emitOn[]
        assertThat(multi.collectItems().asList().await().indefinitely()).containsExactly("JOHN", "JACK", "SUE");
    }

    private String invokeRemoteServiceUsingBlockingIO() {
        try {
            Thread.sleep(300);  // NOSONAR
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "hello";
    }

    private String invokeRemoteServiceUsingBlockingIO(String s) {
        try {
            Thread.sleep(300); // NOSONAR
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return s.toUpperCase();
    }

}
