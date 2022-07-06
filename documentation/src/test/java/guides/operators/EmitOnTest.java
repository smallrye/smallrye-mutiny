package guides.operators;

import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("Convert2MethodRef")
@ExtendWith(SystemOutCaptureExtension.class)
public class EmitOnTest {

    @Test
    public void testEmitOn() {
        // <example>
        Uni<String> uni = Uni.createFrom().<String>emitter(emitter ->
                new Thread(() ->
                        emitter.complete("hello from "
                                + Thread.currentThread().getName())
                ).start()
        )
                .onItem().transform(item -> {
                    // Called on the emission thread.
                    return item.toUpperCase();
                });
        // </example>

        assertThat(uni.await().indefinitely()).startsWith("HELLO FROM").doesNotContain("MAIN");
    }

    @Test
    public void test(SystemOut out) {
        Uni<String> uni = Uni.createFrom().item("hello");
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");

        ExecutorService executor = Executors.newFixedThreadPool(4);
        // <code>
        String res0 = uni.emitOn(executor)
                .onItem()
                .invoke(s -> System.out.println("Received item `" + s + "` on thread: "
                        + Thread.currentThread().getName()))
                .await().indefinitely();

        String res1 = multi.emitOn(executor)
                .onItem()
                .invoke(s -> System.out.println("Received item `" + s + "` on thread: "
                        + Thread.currentThread().getName()))
                .collect().first()
                .await().indefinitely();
        // </code>

        assertThat(out.get()).doesNotContain("main").contains("pool-").contains("hello", "a");
        assertThat(res0).isEqualTo("hello");
        assertThat(res1).isEqualTo("a");
    }
}
