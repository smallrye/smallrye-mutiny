package guides;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

public class ShortcutsTest {


    @Test
    public void test() {

        // <invoke>
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");
        multi.invoke(item -> System.out.println("Received item " + item));
        // </invoke>

        // <call>
        multi.call(item -> executeAnAsyncAction(item));
        // </call>

        multi.subscribe().with(x -> {});
    }

    private Uni<Void> executeAnAsyncAction(String item) {
        return Uni.createFrom().nullItem();
    }

}
