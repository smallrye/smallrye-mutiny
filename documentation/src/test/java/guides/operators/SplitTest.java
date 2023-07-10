package guides.operators;

import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

public class SplitTest {

    // <enum>
    enum Signals {
        INPUT,
        OUTPUT,
        OTHER
    }
    // </enum>

    @Test
    public void splitDemo() {
        // <splits>
        Multi<String> multi = Multi.createFrom().items(
                "!a", "?b", "!c", "!d", "123", "?e"
        );

        var splitter = multi.split(Signals.class, s -> {
            if (s.startsWith("?")) {
                return Signals.INPUT;
            } else if (s.startsWith("!")) {
                return Signals.OUTPUT;
            } else {
                return Signals.OTHER;
            }
        });

        splitter.get(Signals.INPUT)
                .onItem().transform(s -> s.substring(1))
                .subscribe().with(signal -> System.out.println("input - " + signal));

        splitter.get(Signals.OUTPUT)
                .onItem().transform(s -> s.substring(1))
                .subscribe().with(signal -> System.out.println("output - " + signal));

        splitter.get(Signals.OTHER)
                .subscribe().with(signal -> System.out.println("other - " + signal));
        // </splits>
    }
}
