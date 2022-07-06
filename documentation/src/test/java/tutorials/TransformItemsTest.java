package tutorials;

import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("Convert2MethodRef")
@ExtendWith(SystemOutCaptureExtension.class)
public class TransformItemsTest {

    @Test
    public void testUniTransform(SystemOut out) {
        // <uni-transform>
        Uni<String> someUni = Uni.createFrom().item("hello");
        someUni
                .onItem().transform(i -> i.toUpperCase())
                .subscribe().with(
                        item -> System.out.println(item)); // Print HELLO
        // </uni-transform>
        assertThat(out.get()).contains("HELLO");
    }

    @Test
    public void testMultiTransform() {
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");
        // <multi-transform>
        Multi<String> m = multi.onItem().transform(i -> i.toUpperCase());
        // </multi-transform>
        assertThat(m.collect().asList().await().indefinitely()).containsExactly("A", "B", "C");
    }

    @Test
    public void testMultiTransformWithSubscription(SystemOut out) {
        // <multi-transform-2>
        Multi<String> someMulti = Multi.createFrom().items("a", "b", "c");
        someMulti
                .onItem().transform(i -> i.toUpperCase())
                .subscribe().with(
                        item -> System.out.println(item)); // Print A B C
        // </multi-transform-2>
        assertThat(out.get()).contains("A", "B", "C");
    }

    @Test
    public void testChain() {
        Uni<String> uni = Uni.createFrom().item("hello");
        // <chain>
        Uni<String> u = uni
                .onItem().transform(i -> i.toUpperCase())
                .onItem().transform(i -> i + "!");
        // </chain>

        assertThat(u.await().indefinitely()).isEqualTo("HELLO!");

    }
}
