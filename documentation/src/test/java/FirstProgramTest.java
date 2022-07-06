import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemOutCaptureExtension.class)
public class FirstProgramTest {

    @Test
    public void testFirstProgram(SystemOut out) {
        FirstProgram.main(new String[0]);
        assertThat(out.get()).contains(">> HELLO MUTINY");
    }

    @Test
    public void testUniBuilder(SystemOut out) {
        // <uni>
        Uni<String> uni1 = Uni.createFrom().item("hello");
        Uni<String> uni2 = uni1.onItem().transform(item -> item + " mutiny");
        Uni<String> uni3 = uni2.onItem().transform(String::toUpperCase);

        uni3.subscribe().with(item -> System.out.println(">> " + item));
        // </uni>
        assertThat(out.get()).contains("HELLO MUTINY");
        assertThat(uni3.await().indefinitely()).isEqualTo("HELLO MUTINY");
    }

    @Test
    public void testUniBuilder2(SystemOut out) {
        // <uni2>
        Uni<String> uni = Uni.createFrom().item("hello");

        uni.onItem().transform(item -> item + " mutiny");
        uni.onItem().transform(String::toUpperCase);

        uni.subscribe().with(item -> System.out.println(">> " + item));
        // </uni2>
        assertThat(out.get()).contains("hello");
        assertThat(uni.await().indefinitely()).isEqualTo("hello");
    }
}
