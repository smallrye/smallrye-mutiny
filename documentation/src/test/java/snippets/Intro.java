package snippets;

import org.junit.Test;

import io.smallrye.mutiny.Uni;

public class Intro {

    @Test
    public void test() {
        // tag::first[]
        Uni.createFrom().item(() -> "hello")
                .subscribe().with(System.out::println, Throwable::printStackTrace);
        //end::first[]
    }

}
