package io.smallrye.mutiny.helpers.queues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.junit.jupiter.api.Test;

@SuppressWarnings({ "rawtypes", "unchecked", "MismatchedQueryAndUpdateOfCollection" })
public class QueuesTest {

    @Test
    public void testEmptyQueue() {
        Queue<Integer> queue = Queues.<Integer> get(0).get();
        List<Integer> values = Arrays.asList(1, 2, 3);
        assertThat(queue.add(1)).isFalse();
        assertThat(queue.addAll(values)).isFalse();
        assertThat(queue.offer(1)).isFalse();
        assertThat(queue.peek()).isNull();
        assertThat(queue.poll()).isNull();
        assertThat(queue.contains(1)).isFalse();
        assertThat(queue.iterator().hasNext()).isFalse();
        assertThatThrownBy(queue::element).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> queue.remove()).isInstanceOf(NoSuchElementException.class);
        assertThat(queue.remove(1)).isFalse();
        assertThat(queue.containsAll(values)).isFalse();
        assertThat(queue.retainAll(values)).isFalse();
        assertThat(queue.removeAll(values)).isFalse();
        queue.clear();
        assertThat(queue).hasSize(0);
        assertThat(queue.toArray()).isEmpty();
        assertThat(queue.toArray(new Integer[0])).isEmpty();
        Integer[] array = new Integer[] { 4, 5, 6 };
        assertThat(queue.toArray(array)).containsExactly(null, 5, 6);
        assertThat(queue).isEmpty();
    }

    @SuppressWarnings({ "ConstantConditions", "RedundantCollectionOperation",
            "SuspiciousMethodCalls" })
    @Test
    public void testSingletonQueue() {
        SingletonQueue<Integer> queue = new SingletonQueue<>();

        // Keep queue empty for the first tests
        assertThat(queue.isEmpty()).isTrue();
        assertThat(queue.toArray()).isEmpty();
        assertThat(queue.toArray(new Integer[0])).isEmpty();
        Integer[] arr = new Integer[] { 23 };
        assertThat(queue.toArray(arr))
                .hasSize(1).isSameAs(arr);

        arr = new Integer[] { 1, 2, 3 };
        assertThat(queue.toArray(arr))
                .containsExactly(null, 2, 3).isSameAs(arr);

        // Now test when the queue contains one element
        assertThat(queue.add(23)).isTrue();
        assertThat(queue.toArray()).containsExactly(23);
        assertThat(queue.toArray(new Integer[0])).containsExactly(23);
        arr = new Integer[1];
        assertThat(queue.toArray(arr)).containsExactly(23).isSameAs(arr);
        arr = new Integer[] { 1, 2, 3 };
        assertThat(queue.toArray(arr))
                .containsExactly(23, null, 3).isSameAs(arr);

        assertThat(queue.remove()).isEqualTo(23);
        assertThat(queue.isEmpty()).isTrue();
        assertThat(queue.offer(2)).isTrue();
        assertThat(queue.offer(4)).isFalse();
        assertThat(queue).containsExactly(2);
        assertThat(queue.offer(3)).isFalse();
        assertThat(queue).containsExactly(2);

        assertThat(queue.peek()).isEqualTo(2);
        assertThat(queue).containsExactly(2);
        assertThat(queue.poll()).isEqualTo(2);
        assertThat(queue).isEmpty();

        assertThat(queue.add(23)).isTrue();
        assertThat(queue.peek()).isEqualTo(23);
        assertThat(queue.add(24)).isFalse();
        assertThat(queue.peek()).isEqualTo(23);
        assertThat(queue.element()).isEqualTo(23);
        queue.clear();
        assertThat(queue).isEmpty();
        assertThat(queue.peek()).isNull();
        assertThat(queue.poll()).isNull();
        assertThat(queue.element()).isNull();

        assertThat(queue.addAll(Collections.singletonList(2))).isTrue();
        assertThat(queue.addAll(Collections.singletonList(3))).isFalse();
        assertThat(queue.addAll(Arrays.asList(1, 2, 3))).isFalse();
        assertThat(queue.addAll(Collections.emptyList())).isTrue();

        assertThat(queue.remove()).isEqualTo(2);
        queue.add(55);
        assertThat(queue.contains(55)).isTrue();
        assertThat(queue.contains(0)).isFalse();
        assertThat(queue.containsAll(Collections.emptyList())).isTrue();
        assertThat(queue.containsAll(Collections.singleton(55))).isTrue();
        assertThat(queue.containsAll(Arrays.asList(1, 2, 55))).isFalse();

        assertThat(queue.remove(44)).isFalse();
        assertThat(queue.remove("hello")).isFalse();
        assertThat(queue.remove(null)).isFalse();
        assertThat(queue.remove(55)).isTrue();

        // Not supported:
        assertThat(queue.removeAll(Collections.emptyList())).isFalse();
        assertThat(queue.retainAll(Collections.emptyList())).isFalse();

        queue.add(23);
        Iterator<Integer> iterator = queue.iterator();
        assertThat(iterator.hasNext()).isTrue();
        Integer next = iterator.next();
        assertThat(next).isEqualTo(23);
        assertThat(iterator.hasNext()).isFalse();
        assertThat(iterator.next()).isNull();

        iterator = queue.iterator();
        assertThat(iterator.next()).isEqualTo(23);
        iterator.remove();

        iterator = queue.iterator();
        assertThat(iterator.hasNext()).isFalse();
        assertThat(iterator.next()).isNull();
    }
}
