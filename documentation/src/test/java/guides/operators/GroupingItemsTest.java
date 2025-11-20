package guides.operators;

import io.smallrye.mutiny.GroupedMulti;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.Gatherer;
import io.smallrye.mutiny.groups.Gatherers;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GroupingItemsTest {

    @Test
    public void groupIntoLists() {
        // <groupIntoLists>
        Multi<List<Integer>> grouped = Multi.createFrom().range(0, 10)
                .group().intoLists().of(3);  // Group every 3 items into a list

        List<List<Integer>> lists = grouped
                .collect().asList()
                .await().indefinitely();

        assertThat(lists).containsExactly(
                List.of(0, 1, 2),
                List.of(3, 4, 5),
                List.of(6, 7, 8),
                List.of(9)
        );
        // </groupIntoLists>
    }

    @Test
    public void timeBasedListGrouping() {
        // <timeBasedListGrouping>
        Multi<List<Long>> timeWindows = Multi.createFrom()
                .ticks().every(Duration.ofMillis(10))
                .group().intoLists().every(Duration.ofMillis(100));

        // Each list contains items emitted within a 100ms window
        // </timeBasedListGrouping>
    }

    @Test
    public void sizeAndTimeBasedListGrouping() {
        // <sizeAndTimeBasedListGrouping>
        Multi<List<Long>> combined = Multi.createFrom()
                .ticks().every(Duration.ofMillis(10))
                .group().intoLists().of(5, Duration.ofMillis(100));

        // Emits a list when either 5 items are collected OR 100ms expires,
        // whichever happens first
        // </sizeAndTimeBasedListGrouping>
    }

    @Test
    public void groupIntoMultis() {
        // <groupIntoMultis>
        Multi<Multi<Integer>> grouped = Multi.createFrom().range(0, 7)
                .group().intoMultis().of(2);  // Group every 2 items into a Multi

        // Process each Multi independently - for example, join each group
        Multi<String> sums = grouped
                .onItem().transformToUniAndConcatenate(multi ->
                        multi.collect().asList()
                                .onItem().transform(couple -> couple.stream()
                                        .map(Objects::toString)
                                        .collect(Collectors.joining("-"))));

        List<String> results = sums.collect().asList().await().indefinitely();
        assertThat(results).containsExactly("0-1", "2-3", "4-5", "6");
        // </groupIntoMultis>
    }

    @Test
    public void timeBasedGrouping() {
        // <timeBasedGrouping>
        Multi<Multi<Long>> timeWindows = Multi.createFrom()
                .ticks().every(Duration.ofMillis(10))
                .group().intoMultis().every(Duration.ofMillis(100));

        // Each Multi contains items emitted within a 100ms window
        // </timeBasedGrouping>
    }

    @Test
    public void groupByKey() {
        // <groupByKey>
        Multi<GroupedMulti<Integer, Integer>> groups = Multi.createFrom().range(1, 10)
                .group().by(i -> i % 3); // Group by modulo 3

        // Process each group separately
        Uni<List<String>> result = groups
                .onItem().transformToUniAndConcatenate(group ->
                        group.onItem().transform(Objects::toString)
                                .collect().asList()
                                .onItem().transform(l -> group.key() + "(" + String.join(",", l) + ")"))
                .collect().asList();

        List<String> lists = result.await().indefinitely();
        assertThat(lists).containsExactly("1(1,4,7)", "2(2,5,8)", "0(3,6,9)");
        // </groupByKey>
    }

    @Test
    public void groupByKeyAndValue() {
        // <groupByKeyAndValue>
        Multi<GroupedMulti<Boolean, String>> groups = Multi.createFrom().range(1, 10)
                .group().by(
                        i -> i % 2 == 0,           // Key: true for even, false for odd
                        i -> "Number: " + i        // Value: transform to string
                );

        Uni<List<List<String>>> result = groups
                .onItem().transformToUniAndMerge(group ->
                        group.collect().asList()
                )
                .collect().asList();

        List<List<String>> lists = result.await().indefinitely();
        assertThat(lists).hasSize(2);
        // </groupByKeyAndValue>
    }

    @Test
    public void groupByWithMerge() {
        // <groupByWithMerge>
        Multi<String> results = Multi.createFrom().range(1, 100)
                .group().by(i -> i % 10)  // Create 10 groups (0-9)
                .onItem().transformToMulti(
                        group -> group.onItem().transform(i -> "Group " + group.key() + ": " + i)
                )
                .merge(10);  // Set concurrency to at least the number of groups

        List<String> items = results.collect().asList().await().indefinitely();
        assertThat(items).hasSize(99);
        // </groupByWithMerge>
    }

    @Test
    public void groupByDeadlockExample() {
        // <groupByDeadlock>
        // Creating 10 groups but limiting concurrency to only 2
        Multi<String> dangerous = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .group().by(i -> i % 10)  // Creates 10 groups
                .onItem().transformToMulti(
                        group -> group.onItem().transform(i -> "Group " + group.key() + ": " + i)
                )
                .merge(2);  // Only 2 groups can be processed concurrently

        // This may hang because groups 3-10 cannot make progress
        // while waiting for groups 1-2 to complete
        AssertSubscriber<String> sub = dangerous.subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        sub.awaitFailure(t -> assertThat(t).isInstanceOf(BackPressureFailure.class));
        // </groupByDeadlock>
    }

    @Test
    public void groupByWithConcatenate() {
        // <groupByWithConcatenate>
        // Safe alternative: use concatenate to process groups sequentially
        Multi<String> safe = Multi.createFrom().range(1, 100)
                .invoke(i -> System.out.println("emit " + i))
                .group().by(i -> i % 10)
                .onItem().transformToMultiAndConcatenate(
                        group -> group.onItem().transform(i -> "Group " + group.key() + ": " + i)
                                .invoke(i -> System.out.println("group " + i))
                );
        // Groups are processed one at a time, so no deadlock

        List<String> items = safe.collect().asList().await().indefinitely();
        assertThat(items).hasSize(99);
        // </groupByWithConcatenate>
    }

}
