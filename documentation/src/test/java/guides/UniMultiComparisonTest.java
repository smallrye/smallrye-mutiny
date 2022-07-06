package guides;

import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.awaitility.Awaitility.await;

@ExtendWith(SystemOutCaptureExtension.class)
public class UniMultiComparisonTest {

    @Test
    public void comparison(SystemOut out) {
        //<code>
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
        //</code>

        await().until(() -> out.get().contains("Received: C"));
    }

    @Test
    public void conversion(SystemOut out) {
        //<conversion>
        Multi.createFrom().items("a", "b", "c")
          .onItem().transform(String::toUpperCase)
          // Convert the multi to uni
          // It only emits the first item ("a")
          .toUni() 
            .subscribe().with(
              item -> System.out.println("Received: " + item),
              failure -> System.out.println("Failed with " + failure));

        Uni.createFrom().item("a")
          .onItem().transform(String::toUpperCase)
          // Convert the uni to a multi, 
          // the completion event will be fired after "a":
          .toMulti() 
          .subscribe().with(
            item -> System.out.println("Received: " + item),
            failure -> System.out.println("Failed with " + failure)
        );

        //</conversion>

        await().until(() -> out.get().contains("Received: A"));
    }
}
