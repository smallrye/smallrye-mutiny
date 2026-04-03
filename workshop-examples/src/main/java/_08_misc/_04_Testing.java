///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:999-SNAPSHOT
package _08_misc;

import java.io.IOException;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertMulti;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class _04_Testing {

    public static void main(String[] args) {
        System.out.println("⚡️ Testing reactive pipelines with Mutiny test utilities");

        // Testing a Uni
        System.out.println("\n--- UniAssertSubscriber ---");
        UniAssertSubscriber<String> uniSub = Uni.createFrom().item("hello")
                .onItem().transform(String::toUpperCase)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        uniSub.awaitItem()
                .assertItem("HELLO")
                .assertItem(s -> s.length() == 5, "length is 5")
                .inspectItem(item -> System.out.println("Received: " + item));

        // Testing a Multi with AssertSubscriber
        System.out.println("\n--- AssertSubscriber ---");
        AssertSubscriber<Integer> multiSub = Multi.createFrom().range(1, 6)
                .onItem().transform(n -> n * 10)
                .subscribe().withSubscriber(AssertSubscriber.create(10));
        multiSub.awaitCompletion()
                .assertItems(10, 20, 30, 40, 50)
                .assertItemCount(5)
                .assertLastItem(i -> i == 50, "last is 50")
                .inspectItems(items -> System.out.println("Received: " + items));

        // Declarative testing with AssertMulti
        System.out.println("\n--- AssertMulti (declarative) ---");
        AssertMulti.create(Multi.createFrom().range(1, 6))
                .expectNext(1, 2, 3)
                .expectNextMatches(i -> i > 3, "greater than 3")
                .expectNext(5)
                .expectComplete()
                .verify();
        System.out.println("Declarative verification passed!");

        // Testing failures
        System.out.println("\n--- Failure testing ---");
        AssertMulti.create(Multi.createFrom().failure(new IOException("boom")))
                .expectFailure(IOException.class, "boom")
                .verify();
        System.out.println("Failure verification passed!");

        System.out.println("\n✅ All tests passed!");
    }
}
