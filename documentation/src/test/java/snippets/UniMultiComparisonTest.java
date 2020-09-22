package snippets;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class UniMultiComparisonTest {

    @Test
    public void comparison() {
        //tag::code[]
        Multi.createFrom().items("a", "b", "c")
                .onItem().transform(String::toUpperCase)
                .subscribe().with(
                        item -> System.out.println("Received: " + item),
                        failure -> System.out.println("Failed with " + failure.getMessage()));

        Uni.createFrom().item("a")
                .onItem().transform(String::toUpperCase)
                .subscribe().with(
                        item -> System.out.println("Received: " + item),
                        failure -> System.out.println("Failed with " + failure.getMessage()));

        //end::code[]
    }

    @Test
    public void conversion() {
        //tag::conversion[]
        Multi.createFrom().items("a", "b", "c")
                .onItem().transform(String::toUpperCase)
                .toUni() // Convert the multi to uni, only "a" will be forwarded.
                .subscribe().with(
                        item -> System.out.println("Received: " + item),
                        failure -> System.out.println("Failed with " + failure.getMessage()));

        Uni.createFrom().item("a")
                .onItem().transform(String::toUpperCase)
                .toMulti() // Convert the uni to a multi, the completion event will be fired after the emission of "a"
                .subscribe().with(
                        item -> System.out.println("Received: " + item),
                        failure -> System.out.println("Failed with " + failure.getMessage()));

        //end::conversion[]
    }
}
