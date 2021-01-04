package tck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tck.Await.await;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MultiOnFailureResumeTest {

    @Test
    public void onErrorResumeShouldCatchErrorFromSource() {
        AtomicReference<Throwable> exception = new AtomicReference<>();
        Uni<List<String>> uni = Multi.createFrom().<String> failure(
                () -> new QuietRuntimeException("failed"))
                .onFailure().recoverWithItem(err -> {
                    exception.set(err);
                    return "foo";
                })
                .collect().asList();
        assertEquals(await(uni.subscribeAsCompletionStage()), Collections.singletonList("foo"));
        assertEquals(exception.get().getMessage(), "failed");
    }

    @Test
    public void onErrorResumeWithShouldCatchErrorFromSource() {
        AtomicReference<Throwable> exception = new AtomicReference<>();
        assertEquals(await(Multi.createFrom().<String> failure(new QuietRuntimeException("failed"))
                .onFailure().recoverWithMulti(err -> {
                    exception.set(err);
                    return Multi.createFrom().items("foo", "bar");
                })
                .collect().asList()
                .subscribeAsCompletionStage()), Arrays.asList("foo", "bar"));
        assertEquals(exception.get().getMessage(), "failed");
    }

    @Test
    public void onErrorResumeShouldCatchErrorFromStage() {
        AtomicReference<Throwable> exception = new AtomicReference<>();
        assertEquals(await(Multi.createFrom().items("a", "b", "c")
                .map(word -> {
                    if (word.equals("b")) {
                        throw new QuietRuntimeException("failed");
                    }
                    return word.toUpperCase();
                })
                .onFailure().recoverWithItem(err -> {
                    exception.set(err);
                    return "foo";
                })
                .collect().asList()
                .subscribeAsCompletionStage()), Arrays.asList("A", "foo"));
        assertEquals(exception.get().getMessage(), "failed");
    }

    @Test
    public void onErrorResumeWithShouldCatchErrorFromStage() {
        AtomicReference<Throwable> exception = new AtomicReference<>();
        assertEquals(await(Multi.createFrom().items("a", "b", "c")
                .map(word -> {
                    if (word.equals("b")) {
                        throw new QuietRuntimeException("failed");
                    }
                    return word.toUpperCase();
                })
                .onFailure().recoverWithMulti(err -> {
                    exception.set(err);
                    return Multi.createFrom().items("foo", "bar");
                })
                .collect().asList()
                .subscribeAsCompletionStage()), Arrays.asList("A", "foo", "bar"));
        assertEquals(exception.get().getMessage(), "failed");
    }

    @Test
    public void onErrorResumeStageShouldPropagateRuntimeExceptions() {
        assertThrows(RuntimeException.class,
                () -> await(Multi.createFrom().<String> failure(new Exception("source-failure"))
                        .onFailure().recoverWithMulti(t -> {
                            throw new QuietRuntimeException("failed");
                        })
                        .collect().asList()
                        .subscribeAsCompletionStage()));
    }

    @Test
    public void onErrorResumeWithStageShouldPropagateRuntimeExceptions() {
        assertThrows(RuntimeException.class,
                () -> await(Multi.createFrom().<String> failure(new Exception("source-failure"))
                        .onFailure().recoverWithItem(t -> {
                            throw new QuietRuntimeException("failed");
                        })
                        .collect().asList()
                        .subscribeAsCompletionStage()));
    }

    @Test
    public void onErrorResumeWithShouldBeAbleToInjectAFailure() {
        assertThrows(QuietRuntimeException.class,
                () -> await(Multi.createFrom().<String> failure(new QuietRuntimeException("failed"))
                        .onFailure()
                        .recoverWithMulti(err -> Multi.createFrom().failure(new QuietRuntimeException("boom")))
                        .collect().asList()
                        .subscribeAsCompletionStage()));
    }
}
