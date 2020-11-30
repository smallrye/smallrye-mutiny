import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FirstProgramTest {

    @Test
    public void testFirstProgram() {
        FirstProgram.main(new String[0]);
    }

    @Test
    public void testUniBuilder() {
        // tag::uni[]
        Uni<String> uni1 = Uni.createFrom().item("hello");
        Uni<String> uni2 = uni1.onItem().transform(item -> item + " mutiny");
        Uni<String> uni3 = uni2.onItem().transform(String::toUpperCase);

        uni3.subscribe().with(item -> System.out.println(">> " + item));
        // end::uni[]

        assertThat(uni3.await().indefinitely()).isEqualTo("HELLO MUTINY");
    }

    @Test
    public void testUniBuilder2() {
        // tag::uni2[]
        Uni<String> uni = Uni.createFrom().item("hello");

        uni.onItem().transform(item -> item + " mutiny");
        uni.onItem().transform(String::toUpperCase);

        uni.subscribe().with(item -> System.out.println(">> " + item));
        // end::uni2[]

        assertThat(uni.await().indefinitely()).isEqualTo("hello");
    }
}
