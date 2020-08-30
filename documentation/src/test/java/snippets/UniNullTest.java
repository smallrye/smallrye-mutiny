package snippets;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UniNullTest {

    @Test
    public void uni() {
        Uni<String> uni = Uni.createFrom().item(() -> null);
        // tag::code[]
        uni.onItem().ifNull().continueWith("hello");
        uni.onItem().ifNull().switchTo(() -> Uni.createFrom().item("hello"));
        uni.onItem().ifNull().failWith(() -> new Exception("Boom!"));
        // end::code[]

        assertThat(uni.onItem().ifNull().continueWith("hello").await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void uniNotNull() {
        Uni<String> uni = Uni.createFrom().item(() -> null);
        // tag::code-not-null[]
        uni
                .onItem().ifNotNull().transform(String::toUpperCase)
                .onItem().ifNull().continueWith("yolo!");
        // end::code-not-null[]

        String r = uni
                .onItem().ifNotNull().transform(String::toUpperCase)
                .onItem().ifNull().continueWith("yolo!")
                .await().indefinitely();
        assertThat(r).isEqualTo("yolo!");
    }

    @Test
    public void accumulate() {
        Multi<Integer> multi = Multi.createFrom().range(1, 3);
        // tag::acc[]
        Multi<Integer> added = multi.onItem().scan(() -> 0, (item, acc) -> acc + item);
        // end::acc[]
        assertThat(added.subscribe().asIterable()).containsExactly(0, 1, 3);

    }
}
