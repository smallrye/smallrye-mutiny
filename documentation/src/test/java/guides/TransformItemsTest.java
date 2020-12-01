package guides;

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
        // tag::uni-transform[]
        Uni<String> uni = Uni.createFrom().item("hello");
        uni
                .onItem().transform(i -> i.toUpperCase())
                .subscribe().with(
                item -> System.out.println(item)); // Print HELLO
        // end::uni-transform[]
        assertThat(out.get()).contains("HELLO");
    }

    @Test
    public void testMultiTransform() {
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");
        // tag::multi-transform[]
        Multi<String> m = multi
                .onItem().transform(i -> i.toUpperCase());
        // end::multi-transform[]
        assertThat(m.collectItems().asList().await().indefinitely()).containsExactly("A", "B", "C");
    }

    @Test
    public void testMultiTransformWithSubscription(SystemOut out) {
        // tag::multi-transform-2[]
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");
        multi
                .onItem().transform(i -> i.toUpperCase())
                .subscribe().with(
                    item -> System.out.println(item)); // Print A B C
        // end::multi-transform-2[]
        assertThat(out.get()).contains("A", "B", "C");
    }

    @Test
    public void testChain() {
        Uni<String> uni = Uni.createFrom().item("hello");
        // tag::chain[]
        Uni<String> u = uni
                .onItem().transform(i -> i.toUpperCase())
                .onItem().transform(i -> i + "!");
        // end::chain[]

        assertThat(u.await().indefinitely()).isEqualTo("HELLO!");

    }
}
