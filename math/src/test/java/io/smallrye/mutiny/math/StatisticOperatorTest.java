package io.smallrye.mutiny.math;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class StatisticOperatorTest {

    @Test
    public void testWithEmpty() {
        AssertSubscriber<Statistic<Long>> subscriber = Multi.createFrom().<Long> empty()
                .plug(Math.statistics())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.awaitItems(1)
                .awaitCompletion()
                .assertLastItem(new Statistic<>(0, 0.0, 0.0, 0.0, 0.0, null, null));
    }

    @Test
    public void testWithNever() {
        AssertSubscriber<Statistic<Long>> subscriber = Multi.createFrom().<Long> nothing()
                .plug(Math.statistics())
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.cancel();
        subscriber.assertNotTerminated();
        Assertions.assertEquals(0, subscriber.getItems().size());
    }

    @Test
    public void testWithItems() {
        AssertSubscriber<Statistic<Integer>> subscriber = Multi.createFrom().items(1, 2, 3, 4, 2, 5)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.statistics())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .run(() -> {
                    assertThat(subscriber.getItems()).hasSize(3);
                    assertThat(subscriber.getItems().get(0)).satisfies(s -> {
                        assertThat(s.getCount()).isEqualTo(1);
                        assertThat(s.getAverage()).isEqualTo(1.0);
                        assertThat(s.getVariance()).isNaN();
                        assertThat(s.getStandardDeviation()).isNaN();
                        assertThat(s.getSkewness()).isNaN();
                        assertThat(s.getKurtosis()).isNaN();
                        assertThat(s.getMin()).isEqualTo(1);
                        assertThat(s.getMax()).isEqualTo(1);
                    });
                    assertThat(subscriber.getItems().get(1)).satisfies(s -> {
                        assertThat(s.getCount()).isEqualTo(2);
                        assertThat(s.getAverage()).isEqualTo(1.5);
                        assertThat(s.getVariance()).isEqualTo(0.5);
                        assertThat(s.getStandardDeviation()).isCloseTo(0.707107, Offset.offset(0.001));
                        assertThat(s.getSkewness()).isEqualTo(0.0);
                        assertThat(s.getKurtosis()).isEqualTo(-2);
                        assertThat(s.getMin()).isEqualTo(1);
                        assertThat(s.getMax()).isEqualTo(2);
                    });
                    assertThat(subscriber.getItems().get(2)).satisfies(s -> {
                        assertThat(s.getCount()).isEqualTo(3);
                        assertThat(s.getAverage()).isEqualTo(2.0);
                        assertThat(s.getVariance()).isEqualTo(1.0);
                        assertThat(s.getStandardDeviation()).isEqualTo(1.0);
                        assertThat(s.getSkewness()).isEqualTo(0.0);
                        assertThat(s.getKurtosis()).isEqualTo(-1.5);
                        assertThat(s.getMin()).isEqualTo(1);
                        assertThat(s.getMax()).isEqualTo(3);
                    });
                })
                .request(10)
                .awaitItems(6)
                .awaitCompletion();
    }

    @Test
    public void testLatest() {
        Statistic<Long> statistics = Multi.createFrom().items(1L, 2L, 3L, 4L, 2L, 5L)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.statistics())
                .collect().last()
                .await().indefinitely();

        assertThat(statistics.getCount()).isEqualTo(6);
        assertThat(statistics.getMin()).isEqualTo(1L);
        assertThat(statistics.getMax()).isEqualTo(5L);
        assertThat(statistics.getAverage()).isCloseTo(2.833, Offset.offset(0.001));
        assertThat(statistics.getVariance()).isCloseTo(2.167, Offset.offset(0.001));
        assertThat(statistics.getStandardDeviation()).isCloseTo(1.472, Offset.offset(0.001));
        assertThat(statistics.getSkewness()).isCloseTo(0.3053, Offset.offset(0.001));
        assertThat(statistics.getKurtosis()).isCloseTo(-1.152, Offset.offset(0.001));
    }

    @Test
    public void testLatestShuffled() {
        Statistic<Long> statistics = Multi.createFrom().items(5L, 1L, 4L, 3L, 2L, 2L)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.statistics())
                .collect().last()
                .await().indefinitely();

        assertThat(statistics.getCount()).isEqualTo(6);
        assertThat(statistics.getAverage()).isCloseTo(2.833, Offset.offset(0.001));
        assertThat(statistics.getVariance()).isCloseTo(2.167, Offset.offset(0.001));
        assertThat(statistics.getStandardDeviation()).isCloseTo(1.472, Offset.offset(0.001));
        assertThat(statistics.getSkewness()).isCloseTo(0.3053, Offset.offset(0.001));
        assertThat(statistics.getKurtosis()).isCloseTo(-1.152, Offset.offset(0.001));
    }

    @RepeatedTest(1000)
    public void testWithItemsAndFailure() {
        AssertSubscriber<Statistic<Long>> subscriber = Multi.createBy().concatenating().streams(
                Multi.createFrom().items(1L, 2L, 3L, 4L, 2L),
                Multi.createFrom().failure(new Exception("boom")))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .plug(Math.statistics())
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.awaitItems(3)
                .request(10)
                .awaitFailure()
                .run(() -> assertThat(subscriber.getItems()).hasSize(5));
    }
}
