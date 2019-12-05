package tck;

import static tck.Await.await;

import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;

import io.smallrye.mutiny.Multi;

public class MultiFirstSubscriberTckTest extends SubscriberBlackboxVerification<Long> {

    protected MultiFirstSubscriberTckTest() {
        super(new TestEnvironment(10L));
    }

    @Override
    public Long createElement(int element) {
        return (long) element;
    }

    @SuppressWarnings("SubscriberImplementation")
    @Override
    public Subscriber<Long> createSubscriber() {
        CompletableFuture<Subscriber<? super Long>> future = new CompletableFuture<>();
        Subscriber<Long> subscriber = new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                // We need to initially request an element to ensure that we get the publisher.
                subscription.request(1);
            }

            @Override
            public void onNext(Long item) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        };
        Multi.createFrom().items(Multi.createFrom().<Long> publisher(future::complete))
                .flatMap(m -> m)
                .collectItems().first()
                .toMulti()
                .subscribe().withSubscriber(subscriber);
        //noinspection unchecked
        return (Subscriber) await(future);

    }
}
