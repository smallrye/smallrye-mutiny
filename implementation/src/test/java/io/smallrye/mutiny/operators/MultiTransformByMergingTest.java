package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiTransformByMergingTest {

    @Test
    public void testMerging() {
        Multi<Integer> m1 = Multi.createFrom().range(1, 10);
        Multi<Integer> m2 = Multi.createFrom().range(10, 12);

        List<Integer> list = m1.transform().byMergingWith(m2).collectItems().asList().await().indefinitely();
        assertThat(list).hasSize(11);
    }

    @Test
    public void testMergingIterable() {
        Multi<Integer> m1 = Multi.createFrom().range(1, 10);
        Multi<Integer> m2 = Multi.createFrom().range(10, 12);
        Multi<Integer> m3 = Multi.createFrom().range(12, 14);

        List<Integer> list = m1.transform().byMergingWith(Arrays.asList(m2, m3)).collectItems().asList().await()
                .indefinitely();
        assertThat(list).hasSize(13);
    }

    @Test
    public void testMergingWithNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item(1).transform()
                .byMergingWith(Multi.createFrom().item(2), null, Multi.createFrom().item(3)));
    }

    @Test
    public void testMergingWithIterableContainingNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item(1).transform()
                .byMergingWith(Arrays.asList(Multi.createFrom().item(2), null, Multi.createFrom().item(3))));
    }

    @Test
    public void testMergingWithNullIterable() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item(1).transform()
                .byMergingWith((Iterable<Publisher<Integer>>) null));
    }

    @Test
    @Disabled("this test is failing on CI - must be investigated")
    public void testConcurrentEmissionWithMerge() {
        ExecutorService service = Executors.newFixedThreadPool(10);
        Multi<Integer> m1 = Multi.createFrom().range(1, 100).emitOn(service);
        Multi<Integer> m2 = Multi.createFrom().range(100, 150).emitOn(service).emitOn(service).emitOn(service);
        Multi<Integer> m3 = Multi.createFrom().range(150, 200).emitOn(service).emitOn(service);

        Multi<Integer> merged = m1.transform().byMergingWith(m2, m3);
        AssertSubscriber<Integer> subscriber = merged.subscribe()
                .withSubscriber(AssertSubscriber.create(1000));

        subscriber.await();
        List<Integer> items = subscriber.getItems();
        assertThat(Collections.singleton(items)).noneSatisfy(list -> assertThat(list).isSorted());
    }
}
