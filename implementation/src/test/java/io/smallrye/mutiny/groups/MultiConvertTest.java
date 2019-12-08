package io.smallrye.mutiny.groups;

import io.smallrye.mutiny.Multi;
import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiConvertTest {

    @Test
    public void testMultiConvertWithCustomConverter() {
        Multi<String> multi = Multi.createFrom().items(1, 2, 3).convert().with(m -> m.map(i -> Integer.toString(i)));
        List<String> list = multi.collectItems().asList().await().indefinitely();
        assertThat(list).containsExactly("1", "2", "3");
    }

    @Test
    public void testMultiConvertToPublisher() {
        Multi<Integer> items = Multi.createFrom().items(1, 2, 3);
        Publisher<Integer> publisher = items.convert().toPublisher();
        assertThat(items).isSameAs(publisher);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatConverterCannotBeNull() {
        Multi.createFrom().items(1, 2, 3).convert().with(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatUpstreamCannotBeNull() {
        new MultiConvert<>(null);
    }

}