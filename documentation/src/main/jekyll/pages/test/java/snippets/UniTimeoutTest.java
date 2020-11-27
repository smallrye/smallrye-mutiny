package snippets;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

public class UniTimeoutTest {

    @Test
    public void test() {
        Uni<String> uni = Uni.createFrom().nothing();
        // tag::code[]
        String item = uni
                .ifNoItem().after(Duration.ofMillis(100)).recoverWithItem("some fallback item")
                .await().indefinitely();
        // end::code[]

        assertThat(item).isEqualTo("some fallback item");
    }

}
