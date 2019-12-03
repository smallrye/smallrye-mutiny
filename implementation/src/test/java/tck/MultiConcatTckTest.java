package tck;

import static org.testng.Assert.assertEquals;
import static tck.Await.await;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.LongStream;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiConcatTckTest extends AbstractPublisherTck<Long> {
    @Test
    public void concatStageShouldConcatTwoGraphs() {
        assertEquals(await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().items(1, 2, 3),
                        Multi.createFrom().items(4, 5, 6))
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3, 4, 5, 6));
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void concatStageShouldPropagateExceptionsFromSecondStage() {
        await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().items(1, 2, 3),
                        Multi.createFrom().failure(new QuietRuntimeException("failed"))).collectItems().asList()
                        .subscribeAsCompletionStage());
    }

    @Test
    public void concatStageShouldWorkWithEmptyFirstGraph() {
        assertEquals(await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().empty(),
                        Multi.createFrom().items(1, 2, 3))
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3));
    }

    @Test
    public void concatStageShouldWorkWithEmptySecondGraph() {
        assertEquals(await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().items(1, 2, 3),
                        Multi.createFrom().empty())
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3));
    }

    @Test
    public void concatStageShouldWorkWithBothGraphsEmpty() {
        assertEquals(await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().empty(),
                        Multi.createFrom().empty())
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Collections.emptyList());
    }

    @Test
    public void concatStageShouldSupportNestedConcats() {
        assertEquals(await(
                Multi.createBy().concatenating().streams(
                        Multi.createBy().concatenating().streams(
                                Multi.createFrom().items(1, 2, 3),
                                Multi.createFrom().items(4, 5, 6)),
                        Multi.createBy().concatenating().streams(
                                Multi.createFrom().items(7, 8, 9),
                                Multi.createFrom().items(10, 11, 12)))
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        long toEmitFromFirst = elements / 2;

        return Multi.createBy().concatenating().streams(
                Multi.createFrom().iterable(
                        () -> LongStream.rangeClosed(1, toEmitFromFirst).boxed().iterator()),
                Multi.createFrom().iterable(
                        () -> LongStream.rangeClosed(toEmitFromFirst + 1, elements).boxed().iterator()));
    }
}
