import java.util.concurrent.Flow;

import mutiny.zero.ZeroPublisher;

// Run as jbang --cp ${rewritten-mutiny-zero}.jar smoke_test.java
public class smoke_test {

    public static void main(String[] args) {

        Flow.Publisher<String> publisher = ZeroPublisher.fromItems("foo", "bar", "baz");

        publisher.subscribe(new Flow.Subscriber<String>() {

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
                System.out.println(item);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("[done]");
            }
        });
    }
}