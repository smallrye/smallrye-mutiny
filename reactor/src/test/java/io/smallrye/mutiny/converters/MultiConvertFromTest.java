package io.smallrye.mutiny.converters;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.MultiReactorConverters;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MultiConvertFromTest {

    @Test
    public void testCreatingFromAMono() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromMono(), Mono.just(1))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompleted().assertItems(1);
    }

    @Test
    public void testCreatingFromAnEmptyMono() {
        AssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromMono(), Mono.<Void> empty())
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompleted().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAMonoWithFailure() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromMono(), Mono.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromAFlux() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromFlux(), Flux.just(1))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompleted().assertItems(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlux() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromFlux(), Flux.just(1, 2, 3))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(3));

        subscriber.assertCompleted()
                .assertItems(1, 2, 3);
    }

    @Test
    public void testCreatingFromAnEmptyFlux() {
        AssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromFlux(), Flux.<Void> empty())
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompleted().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAFluxWithFailure() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiReactorConverters.fromFlux(), Flux.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertFailedWith(IOException.class, "boom");
    }
}
