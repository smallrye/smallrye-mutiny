package guides;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class BranchingTest {

    @Test
    public void uni() {
        // <pipeline>
        Random random = new Random();
        Uni.createFrom().item(() -> random.nextInt(100))
                .onItem().transformToUni(n -> {
                    if (n % 2 == 0) {
                        return evenOperation(n);
                    } else {
                        return oddOperation(n);
                    }
                })
                .subscribe().with(System.out::println);
        // </pipeline>
    }

    // <branches>
    Uni<String> evenOperation(int n) {
        return Uni.createFrom().item("Even number: " + n)
                .onItem().invoke(() -> System.out.println("(even branch)"));
    }

    Uni<String> oddOperation(int n) {
        return Uni.createFrom().item("Odd number: " + n)
                .onItem().invoke(() -> System.out.println("(odd branch)"));
    }
    // </branches>

    @Test
    public void multi() {
        // <multi-pipeline>
        Random random = new Random();
        Multi.createBy().repeating().supplier(random::nextInt).atMost(20)
                .onItem().transformToUniAndMerge(n -> {
                    System.out.println("----");
                    if (n < 0) {
                        return drop();
                    } else if (n % 2 == 0) {
                        return evenOperation(n);
                    } else {
                        return oddOperation(n);
                    }
                })
                .subscribe().with(str -> System.out.println("=> " + str));
        // </multi-pipeline>
    }

    // <drop>
    Uni<String> drop() {
        return Uni.createFrom().<String>nullItem()
                .onItem().invoke(() -> System.out.println("(dropping negative value)"));
    }
    // </drop>
}
