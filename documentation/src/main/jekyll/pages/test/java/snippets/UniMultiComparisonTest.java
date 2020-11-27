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
            failure -> System.out.println("Failed with " + failure)
        );

        Uni.createFrom().item("a")
          .onItem().transform(String::toUpperCase)
          .subscribe().with(
            item -> System.out.println("Received: " + item),
            failure -> System.out.println("Failed with " + failure)
        );
        //end::code[]
    }

    @Test
    public void conversion() {
        //tag::conversion[]
        Multi.createFrom().items("a", "b", "c")
          .onItem().transform(String::toUpperCase)
          // Convert the multi to uni
          // It only emits the first item ("a")
          .toUni() 
            .subscribe().with(
              item -> System.out.println("Received: " + item),
              failure -> System.out.println("Failed with " + failure);

        Uni.createFrom().item("a")
          .onItem().transform(String::toUpperCase)
          // Convert the uni to a multi, 
          // the completion event will be fired after "a":
          .toMulti() 
          .subscribe().with(
            item -> System.out.println("Received: " + item),
            failure -> System.out.println("Failed with " + failure);

        //end::conversion[]
    }
}
