package guides;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

public class ShortcutsTest {


    @Test
    public void test() {

        // tag::invoke[]
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");
        multi.invoke(item -> System.out.println("Received item " + item));
        // end::invoke[]

        // tag::call[]
        multi.call(item -> executeAnAsyncAction(item));
        // end::call[]

        multi.subscribe().with(x -> {});
    }

    private Uni<Void> executeAnAsyncAction(String item) {
        return Uni.createFrom().nullItem();
    }

}
