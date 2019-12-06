package io.smallrye.mutiny.operators.multi.processors;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.testng.annotations.Test;

import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class UnicastProcessorTest {

    @Test
    public void testTheProcessorCanGetOnlyOneSubscriber() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();
        processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create());
        MultiAssertSubscriber<Integer> second = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create());

        second.assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IllegalStateException.class, null)
                .assertHasNotCompleted();
    }

    @Test
    public void testWithMultithreadedUpstream() {
        UnicastProcessor<String> processor = UnicastProcessor.create();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            int t = i;
            Runnable produce = () -> {
                for (int j = 0; j < 10000; j++) {
                    processor.onNext(t + "-" + j);
                }
            };
            executor.submit(produce);
        }

        MultiAssertSubscriber<Object> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        processor.subscribe(subscriber);

        await().until(() -> subscriber.items().size() == 5 * 10000);
        executor.shutdownNow();
    }

}
