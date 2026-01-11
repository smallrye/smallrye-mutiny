package io.smallrye.mutiny.operators;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MultiContextForkTest {

    @Test
    void checkBoundaries() {
        ArrayList<String> trace = new ArrayList<>();
        Context rootContext = Context.of(123, "yolo");

        List<Integer> result = Uni.createFrom().item(69).toMulti()
                .withContext((multi, ctx) -> {
                    trace.add(ctx.getOrElse(123, () -> "!!!"));
                    trace.add(ctx.getOrElse(456, () -> "!!!"));
                    return multi.onItem().invoke(() -> {
                        trace.add("-" + ctx.getOrElse(123, () -> "!!!"));
                        trace.add("-" + ctx.getOrElse(456, () -> "!!!"));
                    });
                })
                .forkContext()
                .forkContext(ctx -> ctx.put(456, "bar"))
                .withContext((multi, ctx) -> {
                    trace.add(ctx.getOrElse(123, () -> "!!!"));
                    return multi.onItem().invoke(() -> {
                        trace.add("-" + ctx.getOrElse(123, () -> "!!!"));
                    });
                })
                .forkContext(ctx -> ctx.put(123, "foo"))
                .collect().asList().awaitUsing(rootContext).atMost(Duration.ofSeconds(1));

        assertThat(result).containsExactly(69);
        assertThat(trace).containsExactly("foo", "foo", "bar", "-foo", "-bar", "-foo");
        assertThat(rootContext.<String>get(123)).isEqualTo("yolo");
    }

    @Test
    void throwingConsumer() {
        assertThatThrownBy(() ->
                Uni.createFrom().item(69).toMulti()
                        .forkContext(ctx -> {
                            throw new RuntimeException("boom");
                        }).collect().asList().await().atMost(Duration.ofSeconds(1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");
    }
}
