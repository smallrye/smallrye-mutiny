package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.reactivex.Flowable;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiConcatTest {

    @Test
    public void testConcatenationOfSeveralMultis() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating().streams(
                Multi.createFrom().item(5),
                Multi.createFrom().range(1, 3),
                Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1)).subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testConcatenationOfSeveralMultisWithConcurrency() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating()
                .streams(
                        Multi.createFrom().item(5),
                        Multi.createFrom().range(1, 3),
                        Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1))
                .subscribe().withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testConcatenationOfSeveralMultisWithConcurrencyAndDeprecatedApply() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating()
                .streams(
                        Multi.createFrom().item(5),
                        Multi.createFrom().range(1, 3),
                        Multi.createFrom().items(8, 9, 10).onItem().apply(i -> i + 1))
                .subscribe().withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testConcatenationOfSeveralMultisAsIterable() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating().streams(
                Arrays.asList(
                        Multi.createFrom().item(5),
                        Multi.createFrom().range(1, 3),
                        Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1)))
                .subscribe().withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testConcatenationOfSeveralPublishers() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating().streams(
                Flowable.just(5),
                Multi.createFrom().range(1, 3),
                Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1)).subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testConcatenationOfSeveralPublishersAsIterable() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating().streams(
                Arrays.asList(
                        Flowable.just(5),
                        Multi.createFrom().range(1, 3),
                        Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1)))
                .subscribe().withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testMergingEmpty() {
        Multi.createBy().concatenating().streams(Multi.createFrom().empty())
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testMergingWithEmpty() {
        Multi.createBy().concatenating().streams(Multi.createFrom().empty(), Multi.createFrom().item(2))
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompletedSuccessfully().assertReceived(2);
    }

    @Test
    public void testWithFailureCollection() {
        IllegalStateException boom = new IllegalStateException("boom");
        IllegalStateException boom2 = new IllegalStateException("boom2");

        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating().collectFailures().streams(
                Multi.createFrom().item(5),
                Multi.createFrom().failure(boom),
                Multi.createFrom().item(6),
                Multi.createFrom().failure(boom2)).subscribe().withSubscriber(new AssertSubscriber<>(5));

        subscriber.assertTerminated()
                .assertReceived(5, 6)
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "boom2");

        assertThat(subscriber.failures().get(0)).isInstanceOf(CompositeException.class);
        CompositeException ce = (CompositeException) subscriber.failures().get(0);
        assertThat(ce.getCauses()).hasSize(2);

        subscriber = Multi.createBy().concatenating().streams(
                Multi.createFrom().item(5),
                Multi.createFrom().failure(boom),
                Multi.createFrom().item(6),
                Multi.createFrom().failure(boom)).subscribe().withSubscriber(new AssertSubscriber<>(5));

        subscriber.assertTerminated()
                .assertReceived(5)
                .assertHasFailedWith(IllegalStateException.class, "boom");

    }
}
