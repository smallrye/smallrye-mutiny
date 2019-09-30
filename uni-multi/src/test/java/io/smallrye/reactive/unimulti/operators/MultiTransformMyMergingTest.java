package io.smallrye.reactive.unimulti.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.smallrye.reactive.unimulti.Multi;

public class MultiTransformMyMergingTest {

    @Test
    public void testMerging() {
        Multi<Integer> m1 = Multi.createFrom().range(1, 10);
        Multi<Integer> m2 = Multi.createFrom().range(10, 12);

        List<Integer> list = m1.transform().byMergingWith(m2).collect().asList().await().indefinitely();
        assertThat(list).hasSize(11);
    }

    @Test
    public void testMergingIterable() {
        Multi<Integer> m1 = Multi.createFrom().range(1, 10);
        Multi<Integer> m2 = Multi.createFrom().range(10, 12);
        Multi<Integer> m3 = Multi.createFrom().range(12, 14);

        List<Integer> list = m1.transform().byMergingWith(Arrays.asList(m2, m3)).collect().asList().await()
                .indefinitely();
        assertThat(list).hasSize(13);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergingWithNull() {
        Multi.createFrom().item(1).transform()
                .byMergingWith(Multi.createFrom().item(2), null, Multi.createFrom().item(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergingWithIterableContainingNull() {
        Multi.createFrom().item(1).transform()
                .byMergingWith(Arrays.asList(Multi.createFrom().item(2), null, Multi.createFrom().item(3)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergingWithNullIterable() {
        Multi.createFrom().item(1).transform()
                .byMergingWith((Iterable<Publisher<Integer>>) null);
    }
}
