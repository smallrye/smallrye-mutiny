package guides;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("Convert2MethodRef")
public class UncheckedTest {

    @Test
    public void rethrow() {
        Uni<Integer> item = Uni.createFrom().item(1);

        // <rethrow>
        Uni<Integer> uni = item.onItem().transform(i -> {
            try {
                return methodThrowingIoException(i);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        // </rethrow>

        assertThatThrownBy(() -> uni.await().indefinitely())
                .isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class);

    }

    @Test
    public void transformExample() {
        Uni<Integer> item = Uni.createFrom().item(1);

        // <transform>
        Uni<Integer> uni = item.onItem().transform(Unchecked.function(i -> {
            // Can throw checked exception
            return methodThrowingIoException(i);
        }));
        // </transform>

        assertThatThrownBy(() -> uni.await().indefinitely())
                .isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class);

    }

    @Test
    public void invokeExample() {
        Uni<Integer> item = Uni.createFrom().item(1);

        // <invoke>
        Uni<Integer> uni = item.onItem().invoke(Unchecked.consumer(i -> {
            // Can throw checked exception
            throw new IOException("boom");
        }));
        // </invoke>

        assertThatThrownBy(() -> uni.await().indefinitely())
                .isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class);

    }

    private int methodThrowingIoException(Integer ignored) throws IOException {
        throw new IOException("boom");
    }
}
