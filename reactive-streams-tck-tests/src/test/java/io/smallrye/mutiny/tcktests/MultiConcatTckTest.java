package io.smallrye.mutiny.tcktests;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Flow.Publisher;
import java.util.stream.LongStream;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiConcatTckTest extends AbstractPublisherTck<Long> {

    @Test
    public void concatStageShouldConcatTwoGraphs() {
        Assert.assertEquals(Await.await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().items(1, 2, 3),
                        Multi.createFrom().items(4, 5, 6))
                        .collect().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3, 4, 5, 6));
    }

    @Test
    public void concatStageShouldPropagateExceptionsFromSecondStage() {
        Assert.assertThrows(QuietRuntimeException.class, () -> Await.await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().items(1, 2, 3),
                        Multi.createFrom().failure(new QuietRuntimeException("failed"))).collect().asList()
                        .subscribeAsCompletionStage()));
    }

    @Test
    public void concatStageShouldWorkWithEmptyFirstGraph() {
        Assert.assertEquals(Await.await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().empty(),
                        Multi.createFrom().items(1, 2, 3))
                        .collect().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3));
    }

    @Test
    public void concatStageShouldWorkWithEmptySecondGraph() {
        Assert.assertEquals(Await.await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().items(1, 2, 3),
                        Multi.createFrom().empty())
                        .collect().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3));
    }

    @Test
    public void concatStageShouldWorkWithBothGraphsEmpty() {
        Assert.assertEquals(Await.await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().empty(),
                        Multi.createFrom().empty())
                        .collect().asList()
                        .subscribeAsCompletionStage()),
                Collections.emptyList());
    }

    @Test
    public void concatStageShouldSupportNestedConcats() {
        Assert.assertEquals(Await.await(
                Multi.createBy().concatenating().streams(
                        Multi.createBy().concatenating().streams(
                                Multi.createFrom().items(1, 2, 3),
                                Multi.createFrom().items(4, 5, 6)),
                        Multi.createBy().concatenating().streams(
                                Multi.createFrom().items(7, 8, 9),
                                Multi.createFrom().items(10, 11, 12)))
                        .collect().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        long toEmitFromFirst = elements / 2;

        return Multi.createBy().concatenating().streams(
                Multi.createFrom().iterable(
                        () -> LongStream.rangeClosed(1, toEmitFromFirst).boxed().iterator()),
                Multi.createFrom().iterable(
                        () -> LongStream.rangeClosed(toEmitFromFirst + 1, elements).boxed().iterator()));
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }
}
