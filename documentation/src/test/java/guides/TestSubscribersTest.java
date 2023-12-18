package guides;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TestSubscribersTest {

    @Test
    void uni() {
        // <uni>
        Uni<Integer> uni = Uni.createFrom().item(63);

        UniAssertSubscriber<Integer> subscriber = uni
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber
                .awaitItem()
                .assertItem(63);
        // </uni>
    }

    @Test
    void multi() {
        // <multi>
        Multi<Integer> multi = Multi.createFrom().range(1, 5)
                .onItem().transform(n -> n * 10);

        AssertSubscriber<Integer> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .awaitCompletion()
                .assertItems(10, 20, 30, 40);
        // </multi>
    }

    @Test
    void failing() {
        // <failing>
        Multi<Object> multi = Multi.createFrom().failure(() -> new IOException("Boom"));

        AssertSubscriber<Object> subscriber = multi
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .awaitFailure()
                .assertFailedWith(IOException.class, "Boom");
        // </failing>
    }

    @Test
    void uniFunction() {
        // <uni-function>
        Uni<Integer> uni = Uni.createFrom().item(63);

        UniAssertSubscriber<Integer> subscriber = uni
            .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertSubscribed().assertItem(String::valueOf, "63");
        // </uni-function>
    }

    @Test
    void uniFunctionWithObjects() {
        // <uni-function-with-objects>
        class Person {
            String name;
            int age;
        }

        Person person = new Person();
        person.name = "John";
        person.age = 42;
        Uni<Person> uni = Uni.createFrom().item(person);

        UniAssertSubscriber<Person> subscriber = uni
            .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertSubscribed().assertItem(p -> p.name, person.name);
        subscriber.assertSubscribed().assertItem(p -> p.age, person.age);
        // </uni-function-with-objects>
    }
}
