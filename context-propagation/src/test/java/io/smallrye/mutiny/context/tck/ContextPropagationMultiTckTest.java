package io.smallrye.mutiny.context.tck;

import java.util.stream.LongStream;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.Assert;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.context.ContextPropagationMultiInterceptor;

/**
 * Reactive Streams TCK for io.smallrye.mutiny.context.ContextPropagationMultiInterceptor.ContextPropagationMulti.
 */
public class ContextPropagationMultiTckTest extends PublisherVerification<Long> {

    private final ContextPropagationMultiInterceptor interceptor;

    public ContextPropagationMultiTckTest() {
        super(new TestEnvironment(100));
        interceptor = new ContextPropagationMultiInterceptor();
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        Multi<Long> items = Multi.createFrom().items(LongStream.rangeClosed(1, elements).boxed());
        items = interceptor.onMultiCreation(items);
        Assert.assertEquals(items.getClass().getName(),
                "io.smallrye.mutiny.context.ContextPropagationMultiInterceptor$ContextPropagationMulti");
        return items;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        Multi<Long> failed = Multi.createFrom().failure(new RuntimeException("failed"));
        failed = interceptor.onMultiCreation(failed);
        Assert.assertEquals(failed.getClass().getName(),
                "io.smallrye.mutiny.context.ContextPropagationMultiInterceptor$ContextPropagationMulti");
        return failed;
    }
}
