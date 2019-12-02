package tck;

import static tck.Await.await;

import java.util.concurrent.*;
import java.util.function.Function;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

/**
 * Verifies the inner subscriber passed to publishers produced by the mapper function.
 */
public class MultiInnerFlatMapTckTest extends SubscriberWhiteboxVerification<Long> {

    protected MultiInnerFlatMapTckTest() {
        super(new TestEnvironment(10L));
    }

    @Override
    public Long createElement(int element) {
        return (long) element;
    }

    @SuppressWarnings("SubscriberImplementation")
    @Override
    public Subscriber<Long> createSubscriber(WhiteboxSubscriberProbe<Long> probe) {
        CompletableFuture<Subscriber<? super Long>> future = new CompletableFuture<>();
        Subscriber<Long> subscriber = new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                // We need to initially request an element to ensure that we get the publisher.
                subscription.request(1);
                probe.registerOnSubscribe(new SubscriberPuppet() {
                    @Override
                    public void triggerRequest(long elements) {
                        subscription.request(elements);
                    }

                    @Override
                    public void signalCancel() {
                        subscription.cancel();
                    }
                });
            }

            @Override
            public void onNext(Long item) {
                probe.registerOnNext(item);
            }

            @Override
            public void onError(Throwable throwable) {
                probe.registerOnError(throwable);
            }

            @Override
            public void onComplete() {
                probe.registerOnComplete();
            }
        };
        Multi.createFrom().items(Multi.createFrom().<Long> publisher(future::complete))
                .flatMap(Function.identity())
                .subscribe().with(subscriber);
        //noinspection unchecked
        return (Subscriber) await(future);

    }

    @Override
    @Test
    @Ignore("Must be investigated")
    public void required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError() {

    }
}
