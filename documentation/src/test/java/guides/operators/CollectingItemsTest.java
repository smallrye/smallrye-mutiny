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

import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("Convert2MethodRef")
public class CollectingItemsTest {

   @Test
    public void testList() {
       // <list>
       Multi<String> multi = getMulti();
       Uni<List<String>> uni = multi.collect().asList();
       // </list>

       assertThat(uni.await().indefinitely()).containsExactly("a", "b", "c");

       // <first>
       Uni<String> first = multi.collect().first();
       Uni<String> last = multi.collect().last();
       // </first>

       assertThat(first.await().indefinitely()).isEqualTo("a");
       assertThat(last.await().indefinitely()).isEqualTo("c");
   }

   @Test
    public void testMap() {
       // <map>
       Multi<String> multi = getMulti();
       Uni<Map<String, String>> uni =
               multi.collect()
                       .asMap(item -> getUniqueKeyForItem(item));
       // </map>

       assertThat(uni.await().indefinitely()).hasSize(3);
   }

    @Test
    public void testMultiMap() {
        // <multimap>
        Multi<String> multi = getMulti();
        Uni<Map<String, Collection<String>>> uni =
                multi.collect()
                        .asMultiMap(item -> getKeyForItem(item));
        // </multimap>

        assertThat(uni.await().indefinitely()).hasSize(2);
    }

    @Test
    public void testCustomAccumulator() {
        // <accumulator>
        Multi<String> multi = getMulti();
        Uni<MyCollection> uni = multi.collect()
                .in(MyCollection::new, (col, item) -> col.add(item));
        // </accumulator>

        assertThat(uni.await().indefinitely()).hasSize(3);

        // <collector>
        Uni<Long> count = multi.collect()
                .with(Collectors.counting());
        // </collector>

        assertThat(count.await().indefinitely()).isEqualTo(3);
    }

    private String getKeyForItem(String item) {
        if( item.equalsIgnoreCase("b")) {
            return "b";
        } else {
            return "a";
        }
    }

    private String getUniqueKeyForItem(String item) {
        return item;
    }

    private Multi<String> getMulti() {
        return Multi.createFrom().items("a", "b", "c");
    }

    private static class MyCollection extends ArrayList<String> {

    }
}
