package guides.integration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ImperativeToReactiveTest {


    @Test
    public void uniRunSubscriptionOn() {
        // <uni-runSubscriptionOn>
        Uni<String> uni = Uni.createFrom()
                .item(this::invokeRemoteServiceUsingBlockingIO)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        // </uni-runSubscriptionOn>

        String res  = uni.await().indefinitely();
        assertThat(res).isEqualTo("Hello");
    }

    @Test
    public void multiEmitOn() {
        // <multi-emitOn>
        Multi<String> multi = Multi.createFrom().items("john", "jack", "sue")
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transform(this::invokeRemoteServiceUsingBlockingIO);
        // </multi-emitOn>

        List<String> strings = multi.subscribe().asStream().collect(Collectors.toList());
        assertThat(strings).containsExactly("JOHN", "JACK", "SUE");
    }

    public String invokeRemoteServiceUsingBlockingIO() {
        assertThat(Thread.currentThread().getName()).doesNotContain("main");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Hello";
    }

    public String invokeRemoteServiceUsingBlockingIO(String s) {
        assertThat(Thread.currentThread().getName()).doesNotContain("main");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return s.toUpperCase();
    }

}
