package snippets;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class UniBlockingTest {

    @Test
    public void test() {
        // tag::code[]
        Uni<String> blocking = Uni.createFrom().item(this::invokeRemoteServiceUsingBlockingIO)
                .subscribeOn(Infrastructure.getDefaultWorkerPool());
        // end::code[]
        assertThat(blocking.await().indefinitely()).isEqualTo("hello");
    }

    private String invokeRemoteServiceUsingBlockingIO() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "hello";
    }

}
