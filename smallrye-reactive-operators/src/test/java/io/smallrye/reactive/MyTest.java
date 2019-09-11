package io.smallrye.reactive;

import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class MyTest {
    @Test
    public void test() {
        String ret = Uni.createFrom().item("Foo")
            .map(String::toUpperCase)
            // flatMap
            .onItem().mapToUni(str -> Uni.createFrom().item("Bar"))
            .await().indefinitely();
        System.err.println(ret);
        
        Uni.createFrom().deferredItem(() -> "Foo")
            .subscribe().with(System.err::println, Throwable::printStackTrace);
        // Multi has subscribe(Subscriber)
        // .subscribe().with((res, t) -> {})
        
        Uni.createFrom().emitter(sink -> {
           sink.complete("Stef"); 
        }).subscribe().with(System.err::println, Throwable::printStackTrace);
        
        Uni.createFrom().item("Foo")
            .map(s -> { throw new RuntimeException("ouch"); })
            .onFailure(Throwable.class).recoverWithItem(t -> "Fixed: "+t.getMessage())
            .subscribe().with(System.err::println, Throwable::printStackTrace);

        Multi.createFrom().items("one", "two")
            .onItem().mapToItem(String::toUpperCase)
            .subscribe().withSubscriber(new Subscriber<String>() {

                @Override
                public void onSubscribe(Subscription s) {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public void onNext(String t) {
                    System.err.println(t);
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                }

                @Override
                public void onComplete() {
                    // TODO Auto-generated method stub
                    
                }
                
            });
    }
}
