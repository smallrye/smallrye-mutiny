package io.smallrye.mutiny.operators.multi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.groups.Gatherer;
import io.smallrye.mutiny.groups.Gatherer.Extraction;
import io.smallrye.mutiny.groups.Gatherers;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

class MultiGatherTest {

    @Test
    void gatherToLists() {
        AssertSubscriber<List<Integer>> sub = Multi.createFrom().range(1, 100)
                .onItem().gather(Gatherers.window(6))
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.awaitNextItems(2);
        assertThat(sub.getItems()).hasSize(2)
                .anySatisfy(list -> assertThat(list).containsExactly(1, 2, 3, 4, 5, 6))
                .anySatisfy(list -> assertThat(list).containsExactly(7, 8, 9, 10, 11, 12));

        sub.request(Long.MAX_VALUE);
        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(17)
                .anySatisfy(list -> assertThat(list).containsExactly(91, 92, 93, 94, 95, 96))
                .anySatisfy(list -> assertThat(list).containsExactly(97, 98, 99));
    }

    @Test
    void gatherToSlidingWindows() {
        AssertSubscriber<List<Integer>> sub = Multi.createFrom().range(1, 100)
                .onItem().gather(Gatherers.slidingWindow(6))
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.awaitNextItems(2);
        assertThat(sub.getItems()).hasSize(2)
                .anySatisfy(list -> assertThat(list).containsExactly(1, 2, 3, 4, 5, 6))
                .anySatisfy(list -> assertThat(list).containsExactly(2, 3, 4, 5, 6, 7));

        sub.request(Long.MAX_VALUE);
        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(95)
                .anySatisfy(list -> assertThat(list).containsExactly(91, 92, 93, 94, 95, 96))
                .anySatisfy(list -> assertThat(list).containsExactly(92, 93, 94, 95, 96, 97));
    }

    @Test
    void gatherFold() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(1, 100)
                .onItem().gather(Gatherers.fold(() -> 0, Integer::sum))
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.awaitNextItems(1);
        assertThat(sub.getItems()).hasSize(1)
                .last().isEqualTo(4950);

        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(1)
                .last().isEqualTo(4950);
    }

    @Test
    void gatherScan() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(1, 100)
                .onItem().gather(Gatherers.scan(() -> 0, Integer::sum))
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.awaitNextItems(5);
        assertThat(sub.getItems()).hasSize(5)
                .containsExactly(1, 3, 6, 10, 15);

        sub.request(Long.MAX_VALUE);
        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(100)
                .last().isEqualTo(4950);
    }

    @Test
    void gatherLinesOfText() {
        List<String> chunks = List.of(
                "Hello", " ", "world!\n",
                "This is a test\n",
                "==\n==",
                "\n\nThis", " is", " ", "amazing\n\n");
        AssertSubscriber<String> sub = Multi.createFrom().iterable(chunks)
                .onItem().gather()
                .into(StringBuilder::new)
                .accumulate(StringBuilder::append)
                .extract((sb, completed) -> {
                    String str = sb.toString();
                    if (str.contains("\n")) {
                        String[] lines = str.split("\n", 2);
                        return Optional.of(Extraction.of(new StringBuilder(lines[1]), lines[0]));
                    }
                    return Optional.empty();
                })
                .finalize(sb -> Optional.of(sb.toString()))
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.awaitNextItems(2);
        assertThat(sub.getItems()).containsExactly("Hello world!", "This is a test");

        sub.request(Long.MAX_VALUE);
        assertThat(sub.getItems()).containsExactly(
                "Hello world!",
                "This is a test",
                "==",
                "==",
                "",
                "This is amazing",
                "",
                "");
    }

    @Test
    void checkCompletionCorrectness() {
        List<String> chunks = List.of(
                "a", "1,b1,c1,d1");
        AssertSubscriber<String> sub = Multi.createFrom().iterable(chunks)
                .onItem().gather()
                .into(StringBuilder::new)
                .accumulate(StringBuilder::append)
                .extract((sb, completed) -> {
                    String str = sb.toString();
                    if (str.contains(",")) {
                        String[] lines = str.split(",", 2);
                        return Optional.of(Extraction.of(new StringBuilder(lines[1]), lines[0]));
                    }
                    return Optional.empty();
                })
                .finalize(sb -> Optional.of(sb.toString()))
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.awaitNextItem().assertNotTerminated();
        assertThat(sub.getItems()).containsExactly("a1");

        sub.awaitNextItem().assertNotTerminated();
        assertThat(sub.getItems()).containsExactly("a1", "b1");

        sub.awaitNextItem().assertNotTerminated();
        assertThat(sub.getItems()).containsExactly("a1", "b1", "c1");

        sub.awaitNextItem().assertCompleted();
        assertThat(sub.getItems()).containsExactly("a1", "b1", "c1", "d1");
    }

    @Test
    void rejectNullParamsInApi() {
        Multi<Integer> multi = Multi.createFrom().range(1, 100);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> multi.onItem().gather().into(null))
                .withMessageContaining("initialAccumulatorSupplier");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> multi.onItem().gather().into(ArrayList<Integer>::new).accumulate(null))
                .withMessageContaining("accumulator");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> multi.onItem().gather().into(ArrayList<Integer>::new).accumulate((a, b) -> a)
                        .extract(
                                (BiFunction<ArrayList<Integer>, Boolean, Optional<Extraction<ArrayList<Integer>, Object>>>) null))
                .withMessageContaining("extractor");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> multi.onItem().gather().into(ArrayList<Integer>::new).accumulate((a, b) -> a)
                        .extract((a, completed) -> Optional.empty()).finalize(null))
                .withMessageContaining("finalizer");
    }

    @Test
    void rejectNullInInitialAccumulatorSupplier() {
        AssertSubscriber<Object> sub = Multi.createFrom().range(1, 100)
                .onItem().gather()
                .into(() -> null)
                .accumulate((acc, next) -> "")
                .extract((acc, completed) -> Optional.empty())
                .finalize(acc -> Optional.empty())
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(NullPointerException.class, "The initial accumulator cannot be null");
    }

    @Test
    void rejectNullInAccumulator() {
        AssertSubscriber<Object> sub = Multi.createFrom().range(1, 100)
                .onItem().gather()
                .into(ArrayList::new)
                .accumulate((acc, next) -> null)
                .extract((acc, completed) -> Optional.empty())
                .finalize(acc -> Optional.empty())
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(NullPointerException.class, "The accumulator returned a null value");
    }

    @Test
    void rejectNullInExtractor() {
        AssertSubscriber<Object> sub = Multi.createFrom().range(1, 100)
                .onItem().gather()
                .into(ArrayList::new)
                .accumulate((acc, next) -> {
                    acc.add(next);
                    return acc;
                })
                .extract((acc, completed) -> null)
                .finalize(acc -> Optional.empty())
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(NullPointerException.class, "The extractor returned a null value");
    }

    @Test
    void rejectNullInExtractorOptionalTupleLeft() {
        AssertSubscriber<Object> sub = Multi.createFrom().range(1, 100)
                .onItem().gather()
                .into(ArrayList::new)
                .accumulate((acc, next) -> {
                    acc.add(next);
                    return acc;
                })
                .extract((acc, completed) -> Optional.of(Extraction.of(null, "ok")))
                .finalize(acc -> Optional.empty())
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(NullPointerException.class, "The extractor returned a null accumulator value");
    }

    @Test
    void rejectNullInExtractorOptionalTupleRight() {
        AssertSubscriber<Object> sub = Multi.createFrom().range(1, 100)
                .onItem().gather()
                .into(ArrayList::new)
                .accumulate((acc, next) -> {
                    acc.add(next);
                    return acc;
                })
                .extract((acc, completed) -> Optional.of(Extraction.of(new ArrayList<>(), null)))
                .finalize(acc -> Optional.empty())
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(NullPointerException.class, "The extractor returned a null value to emit");
    }

    @Test
    void rejectNullInFinalizer() {
        AssertSubscriber<Object> sub = Multi.createFrom().range(1, 100)
                .onItem().gather()
                .into(ArrayList::new)
                .accumulate((acc, next) -> {
                    acc.add(next);
                    return acc;
                })
                .extract((acc, completed) -> Optional.empty())
                .finalize(acc -> null)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(NullPointerException.class, "The finalizer returned a null value");
    }

    @Test
    void rejectExceptionInInitialAccumulatorSupplier() {
        AssertSubscriber<Object> sub = Multi.createFrom().range(1, 100)
                .onItem().gather()
                .into(() -> {
                    throw new RuntimeException("boom");
                })
                .accumulate((acc, next) -> "")
                .extract((acc, completed) -> Optional.empty())
                .finalize(acc -> Optional.empty())
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    void rejectExceptionInAccumulator() {
        AssertSubscriber<Object> sub = Multi.createFrom().range(1, 100)
                .onItem().gather()
                .into(ArrayList::new)
                .accumulate((acc, next) -> {
                    throw new RuntimeException("boom");
                })
                .extract((acc, completed) -> Optional.empty())
                .finalize(acc -> Optional.empty())
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    void rejectExceptionInExtractor() {
        AssertSubscriber<Object> sub = Multi.createFrom().range(1, 100)
                .onItem().gather()
                .into(ArrayList::new)
                .accumulate((acc, next) -> {
                    acc.add(next);
                    return acc;
                })
                .extract((acc, completed) -> {
                    throw new RuntimeException("boom");
                })
                .finalize(acc -> Optional.empty())
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    void rejectExceptionInFinalizer() {
        AssertSubscriber<Object> sub = Multi.createFrom().range(1, 100)
                .onItem().gather()
                .into(ArrayList::new)
                .accumulate((acc, next) -> {
                    acc.add(next);
                    return acc;
                })
                .extract((acc, completed) -> Optional.empty())
                .finalize(acc -> {
                    throw new RuntimeException("boom");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    void errorHandling() {
        AssertSubscriber<Object> sub = Multi.createFrom().items("foo", "bar")
                .onItem().transformToMultiAndConcatenate(s -> Multi.createFrom().failure(() -> new RuntimeException("boom")))
                .onItem().gather()
                .into(StringBuilder::new)
                .accumulate(StringBuilder::append)
                .extract((sb, completed) -> Optional.empty())
                .finalize(acc -> Optional.of(acc.toString()))
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        sub.assertFailedWith(RuntimeException.class, "boom");
        sub.request(Long.MAX_VALUE);
        sub.assertHasNotReceivedAnyItem();
    }

    @Test
    void rejectBadRequests() {
        Multi<Object> multi = Multi.createFrom().items("foo", "bar")
                .onItem().gather()
                .into(StringBuilder::new)
                .accumulate(StringBuilder::append)
                .extract((sb, completed) -> Optional.empty())
                .finalize(acc -> Optional.of(acc.toString()));

        AssertSubscriber<Object> sub = multi.subscribe().withSubscriber(AssertSubscriber.create());
        sub.request(0L).assertFailedWith(IllegalArgumentException.class,
                "The number of items requested must be strictly positive");

        sub = multi.subscribe().withSubscriber(AssertSubscriber.create());
        sub.request(-10L).assertFailedWith(IllegalArgumentException.class,
                "The number of items requested must be strictly positive");
    }

    @Test
    void builderApi() {
        Gatherer<String, StringBuilder, String> gatherer = Gatherers.<String> builder()
                .into(StringBuilder::new)
                .accumulate(StringBuilder::append)
                .extract((sb, completed) -> {
                    String str = sb.toString();
                    if (str.contains("\n")) {
                        String[] lines = str.split("\n", 2);
                        return Optional.of(Extraction.of(new StringBuilder(lines[1]), lines[0]));
                    }
                    return Optional.empty();
                })
                .finalize(sb -> Optional.of(sb.toString()));

        List<String> chunks = List.of(
                "Hello", " ", "world!\n",
                "This is a test\n",
                "==\n==",
                "\n\nThis", " is", " ", "amazing\n\n");
        AssertSubscriber<String> sub = Multi.createFrom().iterable(chunks)
                .onItem().gather(gatherer)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.awaitNextItems(2);
        assertThat(sub.getItems()).containsExactly("Hello world!", "This is a test");

        sub.request(Long.MAX_VALUE);
        assertThat(sub.getItems()).containsExactly(
                "Hello world!",
                "This is a test",
                "==",
                "==",
                "",
                "This is amazing",
                "",
                "");
    }
}
