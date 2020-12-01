package guides.operators;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@SuppressWarnings("Convert2MethodRef")
public class CollectingItemsTest {

   @Test
    public void testList() {
       // tag::list[]
       Multi<String> multi = getMulti();
       Uni<List<String>> uni = multi.collectItems().asList();
       // end::list[]

       assertThat(uni.await().indefinitely()).containsExactly("a", "b", "c");
   }

   @Test
    public void testMap() {
       // tag::map[]
       Multi<String> multi = getMulti();
       Uni<Map<String, String>> uni =
               multi.collectItems()
                       .asMap(item -> getKeyForItem(item));
       // end::map[]

       assertThat(uni.await().indefinitely()).hasSize(2);
   }

    @Test
    public void testMultiMap() {
        // tag::multimap[]
        Multi<String> multi = getMulti();
        Uni<Map<String, Collection<String>>> uni =
                multi.collectItems()
                        .asMultiMap(item -> getKeyForItem(item));
        // end::multimap[]

        assertThat(uni.await().indefinitely()).hasSize(2);
    }

    @Test
    public void testCustomAccumulator() {
        // tag::accumulator[]
        Multi<String> multi = getMulti();
        Uni<MyCollection> uni = multi.collectItems()
                .in(MyCollection::new, (col, item) -> col.add(item));
        // end::accumulator[]

        assertThat(uni.await().indefinitely()).hasSize(3);

        // tag::collector[]
        Uni<Long> count = multi.collectItems()
                .with(Collectors.counting());
        // end::collector[]

        assertThat(count.await().indefinitely()).isEqualTo(3);
    }

    private String getKeyForItem(String item) {
        if( item.equalsIgnoreCase("b")) {
            return "b";
        } else {
            return "a";
        }
    }

    private Multi<String> getMulti() {
        return Multi.createFrom().items("a", "b", "c");
    }

    private static class MyCollection extends ArrayList<String> {

    }
}
