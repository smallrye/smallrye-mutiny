package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;

public class MultiTransformByMergingTest {

    @Test
    public void testMerging() {
        Multi<Integer> m1 = Multi.createFrom().range(1, 10);
        Multi<Integer> m2 = Multi.createFrom().range(10, 12);

        List<Integer> list = Multi.createBy().merging().streams(m1, m2).collect().asList().await().indefinitely();
        assertThat(list).hasSize(11);
    }

    @Test
    public void testMergingIterable() {
        Multi<Integer> m1 = Multi.createFrom().range(1, 10);
        Multi<Integer> m2 = Multi.createFrom().range(10, 12);
        Multi<Integer> m3 = Multi.createFrom().range(12, 14);

        List<Integer> list = Multi.createBy().merging().streams(m1, m2, m3).collect().asList().await()
                .indefinitely();
        assertThat(list).hasSize(13);
    }

    @Test
    public void testMergingWithNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createBy().merging().streams(
                Multi.createFrom().item(1),
                Multi.createFrom().item(2),
                null,
                Multi.createFrom().item(3)));
    }
}
