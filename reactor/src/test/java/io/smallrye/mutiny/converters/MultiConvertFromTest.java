package io.smallrye.mutiny.converters;

import java.io.IOException;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.MultiReactorConverters;
import io.smallrye.mutiny.test.MultiAssertSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MultiConvertFromTest {

    @Test
    public void testCreatingFromAMono() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromMono(), Mono.just(1))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAnEmptyMono() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromMono(), Mono.<Void> empty())
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAMonoWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromMono(), Mono.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromAFlux() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromFlux(), Flux.just(1))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlux() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromFlux(), Flux.just(1, 2, 3))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(3));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(1, 2, 3);
    }

    @Test
    public void testCreatingFromAnEmptyFlux() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromFlux(), Flux.<Void> empty())
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAFluxWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromFlux(), Flux.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }
}
