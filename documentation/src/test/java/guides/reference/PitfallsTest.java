package guides.reference;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PitfallsTest {

    private Uni<String> fetch(String id) {
        return Uni.createFrom().item("Yolo");
    }

    @Test
    public void noMagicScheduling() {
        // <noMagicJoin>
        Uni<List<String>> data = Uni.join().all(
                fetch("abc"),
                fetch("def"),
                fetch("123")).andFailFast();
        // </noMagicJoin>
    }

    @Test
    public void inMemoryData() {
        // <inMemoryData>
        Uni<Integer> uni = Uni.createFrom().item(123);
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3, 4, 5);
        // </inMemoryData>
    }

    private List<String> obtainValues(String key) {
        return List.of();
    }

    // <suspiciousPublisher>
    public Multi<String> fetchData(String key) {
        List<String> strings = obtainValues(key);
        return Multi.createFrom().iterable(strings);
    }
    // </suspiciousPublisher>

    private Multi<String> streamData(String key) {
        return Multi.createFrom().items("");
    }

    private Multi<String> extractData(String line) {
        return Multi.createFrom().items("");
    }

    private Multi<String> flatmap() {
        // <flatmap-ism>
        Multi<String> stream = streamData("abc");
        return stream.onItem().transformToMultiAndMerge(line -> {
            if (line.trim().length() > 10) {
                return extractData(line);
            } else {
                return Multi.createFrom().item("[N/A]");
            }
        });
        // </flatmap-ism>
    }
}
