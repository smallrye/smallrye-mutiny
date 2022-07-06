package tutorials;

import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("Convert2MethodRef")
@ExtendWith(SystemOutCaptureExtension.class)
public class TransformItemsAsyncTest {

    interface MyAsyncService {
        // <service>
        Uni<String> invokeRemoteGreetingService(String name);
        // </service>
    }

    static class MyAsyncServiceImpl implements MyAsyncService {

        @Override
        public Uni<String> invokeRemoteGreetingService(String name) {
            return Uni.createFrom().item(name);
        }
    }

    MyAsyncServiceImpl service = new MyAsyncServiceImpl();


    private Uni<String> invokeRemoteGreetingService(String name) {
        return service.invokeRemoteGreetingService(name);
    }

    @Test
    public void testCall() {

        Uni<String> uni = Uni.createFrom().item("mutiny");
        // <call>
        Uni<String> result = uni
            .onItem().transformToUni(name -> invokeRemoteGreetingService(name));
        // </call>

        assertThat(result.await().indefinitely()).isEqualTo("mutiny");
    }

    @Test
    public void testChain(SystemOut out) {
        // <chain>
        Uni<String> uni = Uni.createFrom().item("Cameron");
        uni
            .onItem().transformToUni(name -> invokeRemoteGreetingService(name))
            .subscribe().with(
                    item -> System.out.println(item), // Print "Hello Cameron",
                    fail -> fail.printStackTrace()); // Print the failure stack trace
        // </chain>
        assertThat(out.get()).contains("Cameron");
    }


    @Test
    public void testUniToMulti(SystemOut out) {
        Uni<String> uni = Uni.createFrom().item("Cameron");
        // <uni-to-multi>
        Multi<String> result = uni
            .onItem().transformToMulti(item -> Multi.createFrom().items(item, item));
        // </uni-to-multi>
        assertThat(result.collect().asList().await().indefinitely()).containsExactly("Cameron", "Cameron");

        // <uni-to-multi-2>
        uni
            .onItem().transformToMulti(item -> Multi.createFrom().items(item, item))
            .subscribe().with(
                    item -> System.out.println(item)); // Called twice
        // </uni-to-multi-2>
        assertThat(out.get()).contains("Cameron");
    }

    @Test
    public void testMergeAndConcatUni() {
        Multi<String> multi = Multi.createFrom().items("a", "b");

        // <merge-concat>
        Multi<String> merged = multi
            .onItem().transformToUniAndMerge(name -> invokeRemoteGreetingService(name));

        Multi<String> concat = multi
            .onItem().transformToUniAndConcatenate(name -> invokeRemoteGreetingService(name));
        // </merge-concat>

        assertThat(merged.collect().asList().await().indefinitely()).containsExactly("a", "b");
        assertThat(concat.collect().asList().await().indefinitely()).containsExactly("a", "b");
    }

    @Test
    public void testMergeAndConcatMulti() {
        Multi<String> multi = Multi.createFrom().items("a", "b");

        // <merge-concat-multi>
        Multi<String> merged = multi
            .onItem().transformToMultiAndMerge(item -> someMulti(item));

        Multi<String> concat = multi
            .onItem().transformToMultiAndConcatenate(item -> someMulti(item));
        // </merge-concat-multi>

        assertThat(merged.collect().asList().await().indefinitely()).containsExactly("a", "b");
        assertThat(concat.collect().asList().await().indefinitely()).containsExactly("a", "b");
    }

    private Multi<String> someMulti(String item) {
        return Multi.createFrom().item(item);
    }
}
