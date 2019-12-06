package snippets;

import io.smallrye.mutiny.Uni;
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeoutTest {

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
