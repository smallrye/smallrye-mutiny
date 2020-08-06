package io.smallrye.mutiny.context.tck;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;

import io.smallrye.mutiny.context.ContextPropagationMultiInterceptor;

public class ContextPropagationSubscriberTckTest extends SubscriberWhiteboxVerification<Long> {

    private final ContextPropagationMultiInterceptor interceptor;

    protected ContextPropagationSubscriberTckTest() {
        super(new TestEnvironment(100));
        interceptor = new ContextPropagationMultiInterceptor();
    }

    @Override
    public Long createElement(int i) {
        return (long) i;
    }

    @SuppressWarnings({ "unchecked", "ReactiveStreamsSubscriberImplementation" })
    @Override
    public Subscriber<Long> createSubscriber(WhiteboxSubscriberProbe<Long> probe) {
        return (Subscriber<Long>) interceptor.onSubscription(null, new Subscriber<Long>() {

            private final AtomicReference<Subscription> upstream = new AtomicReference<>();

            @Override
            public void onSubscribe(Subscription subscription) {
                // While the TCK expect a single subscriber, it's not necessary the case (broadcast for instance)
                // So cancel the second subscription here.
                if (upstream.compareAndSet(null, subscription)) {
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
                } else {
                    subscription.cancel();
                }
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
        });
    }
}
