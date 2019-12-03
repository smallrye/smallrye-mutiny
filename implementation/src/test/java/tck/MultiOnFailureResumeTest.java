package tck;

import static org.testng.Assert.assertEquals;
import static tck.Await.await;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

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
                .collectItems().asList();
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
                .collectItems().asList()
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
                .collectItems().asList()
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
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList("A", "foo", "bar"));
        assertEquals(exception.get().getMessage(), "failed");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void onErrorResumeStageShouldPropagateRuntimeExceptions() {
        await(Multi.createFrom().<String> failure(new Exception("source-failure"))
                .onFailure().recoverWithMulti(t -> {
                    throw new QuietRuntimeException("failed");
                })
                .collectItems().asList()
                .subscribeAsCompletionStage());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void onErrorResumeWithStageShouldPropagateRuntimeExceptions() {
        await(Multi.createFrom().<String> failure(new Exception("source-failure"))
                .onFailure().recoverWithItem(t -> {
                    throw new QuietRuntimeException("failed");
                })
                .collectItems().asList()
                .subscribeAsCompletionStage());
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = ".*boom.*")
    public void onErrorResumeWithShouldBeAbleToInjectAFailure() {
        await(Multi.createFrom().<String> failure(new QuietRuntimeException("failed"))
                .onFailure().recoverWithMulti(err -> Multi.createFrom().failure(new QuietRuntimeException("boom")))
                .collectItems().asList()
                .subscribeAsCompletionStage());
    }
}
