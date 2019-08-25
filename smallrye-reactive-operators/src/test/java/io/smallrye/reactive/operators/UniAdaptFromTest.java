package io.smallrye.reactive.operators;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.smallrye.reactive.Uni;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class UniAdaptFromTest {

    @Test
    public void testCreatingFromACompletable() {
        Uni<Void> uni = Uni.createFrom().converterOf(Completable.complete());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromACompletableFromVoid() {
        Uni<Void> uni = Uni.createFrom().converterOf(Completable.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromASingle() {
        Uni<Integer> uni = Uni.createFrom().converterOf(Single.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromASingleWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converterOf(Single.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAMaybe() {
        Uni<Integer> uni = Uni.createFrom().converterOf(Maybe.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyMaybe() {
        Uni<Void> uni = Uni.createFrom().converterOf(Maybe.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAMaybeWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converterOf(Maybe.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAFlowable() {
        Uni<Integer> uni = Uni.createFrom().converterOf(Flowable.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlowable() {
        Uni<Integer> uni = Uni.createFrom().converterOf(Flowable.just(1, 2, 3));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyFlowable() {
        Uni<Void> uni = Uni.createFrom().converterOf(Flowable.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAFlowableWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converterOf(Flowable.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAMono() {
        Uni<Integer> uni = Uni.createFrom().converterOf(Mono.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyMono() {
        Uni<Void> uni = Uni.createFrom().converterOf(Mono.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAMonoWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converterOf(Mono.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAFlux() {
        Uni<Integer> uni = Uni.createFrom().converterOf(Flux.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlux() {
        Uni<Integer> uni = Uni.createFrom().converterOf(Flux.just(1, 2, 3));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyFlux() {
        Uni<Void> uni = Uni.createFrom().converterOf(Flux.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAFluxWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converterOf(Flux.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromCompletionStages() {
        CompletableFuture<Integer> valued = CompletableFuture.completedFuture(1);
        CompletableFuture<Void> empty = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> boom = new CompletableFuture<>();
        boom.completeExceptionally(new Exception("boom"));

        Uni<Integer> u1 = Uni.createFrom().completionStage(valued);
        Uni<Void> u2 = Uni.createFrom().completionStage(empty);
        Uni<Void> u3 = Uni.createFrom().converterOf(Uni.createFrom().completionStage(boom));

        assertThat(u1.await().asOptional().indefinitely()).contains(1);
        assertThat(u2.await().indefinitely()).isEqualTo(null);
        try {
            u3.await();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(Exception.class);
        }
    }

}