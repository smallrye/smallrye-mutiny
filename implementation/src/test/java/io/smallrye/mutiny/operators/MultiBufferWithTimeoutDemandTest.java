package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.MultiGroupTest.BufferWithTimeoutStaleTimerTest.ControllableScheduler;
import io.smallrye.mutiny.operators.multi.MultiBufferWithTimeoutOp;
import io.smallrye.mutiny.subscription.MultiEmitter;

/**
 * Demand accounting of {@code group().intoLists().of(size, duration)}: downstream demand is expressed in lists.
 * Every test uses the controllable scheduler from {@link MultiGroupTest}: timers only run when the test runs them.
 */
class MultiBufferWithTimeoutDemandTest {

    private static final int SIZE = 10;

    private ControllableScheduler scheduler;
    private AtomicReference<MultiEmitter<? super Integer>> emitter;
    private AtomicLong upstreamRequested;

    @BeforeEach
    void init() {
        scheduler = new ControllableScheduler();
        emitter = new AtomicReference<>();
        upstreamRequested = new AtomicLong();
    }

    private AssertSubscriber<List<Integer>> subscribe(int size, long initialRequest) {
        Multi<Integer> upstream = Multi.createFrom().<Integer> emitter(emitter::set)
                .onRequest().invoke(upstreamRequested::addAndGet);
        return new MultiBufferWithTimeoutOp<>(upstream, size, Duration.ofHours(1), scheduler, false)
                .subscribe().withSubscriber(AssertSubscriber.create(initialRequest));
    }

    private void emit(int item) {
        emitter.get().emit(item);
    }

    private void emit(int from, int to) {
        for (int i = from; i <= to; i++) {
            emitter.get().emit(i);
        }
    }

    private void fireLatestTimer() {
        scheduler.runTask(scheduler.taskCount() - 1);
    }

    /**
     * Lists never exceed size
     */
    @Test
    void listsMustNeverExceedSize() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 2);

        // Two early timer flushes of 1 item each use up the demand for 2 lists...
        emit(0);
        fireLatestTimer();
        emit(1);
        fireLatestTimer();
        assertThat(sub.getItems()).containsExactly(List.of(0), List.of(1));

        // ...while upstream was granted 20 items: the remaining 18 still arrive
        emit(2, 19);
        sub.request(1);

        for (List<Integer> list : sub.getItems()) {
            assertThat(list.size()).isLessThanOrEqualTo(SIZE);
        }
    }

    /**
     * No early flush on request()
     */
    @Test
    void requestMustNotFlushBatchBeforeItIsFullOrTimedOut() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 2);

        emit(0, 12);
        sub.assertNotTerminated();
        assertThat(sub.getItems()).containsExactly(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        // 10, 11, 12 form a batch that is neither full nor timed out: more demand must not flush it
        sub.request(1);
        assertThat(sub.getItems()).hasSize(1);

        // It is emitted when its timer fires
        fireLatestTimer();
        assertThat(sub.getItems()).containsExactly(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), List.of(10, 11, 12));
    }

    // Javadoc: "When the upstream Multi sends the completion event, the produced Multi emits the current list,
    // and sends the completion event. This last list may not contain size items. If the upstream Multi sends the
    // completion event before having emitted any event, the completion event is propagated immediately."

    /**
     * Completion emits current list then completes
     */
    @Test
    void completionEmitsCurrentPartialListThenCompletes() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 1);

        emit(0, 2);
        sub.assertNotTerminated();

        emitter.get().complete();
        // The current list is emitted although it does not contain SIZE items, then the completion
        assertThat(sub.getItems()).containsExactly(List.of(0, 1, 2));
        sub.assertCompleted();
    }

    /**
     * Completion propagates immediately without any list and without any demand
     */
    @Test
    void completionRightAfterAFullListDoesNotEmitAnEmptyList() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 1);

        emit(0, 9);
        assertThat(sub.getItems()).containsExactly(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        // Nothing is pending: the completion is propagated immediately, without demand and without an empty list
        emitter.get().complete();
        assertThat(sub.getItems()).hasSize(1);
        sub.assertCompleted();
    }

    /**
     * Completion before any items propagates immediately
     */
    @Test
    void completionBeforeAnyItemIsPropagatedImmediately() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 0);

        // No demand, no item: the completion does not wait for a request
        emitter.get().complete();
        assertThat(sub.getItems()).isEmpty();
        sub.assertCompleted();
    }

    /**
     * Completion respects demand (finite size)
     * <p>
     * Main fails with BackPressureFailure: Cannot emit item due to lack of requests
     */
    @Test
    void completionMustNotEmitFinalListWithoutDemand() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 2);

        emit(0);
        fireLatestTimer();
        emit(1);
        fireLatestTimer();
        // Demand exhausted, then the final items arrive and upstream completes
        emit(2, 3);
        emitter.get().complete();

        assertThat(sub.getItems()).containsExactly(List.of(0), List.of(1));
        sub.assertNotTerminated();

        sub.request(1);
        assertThat(sub.getItems()).containsExactly(List.of(0), List.of(1), List.of(2, 3));
        sub.assertCompleted();
    }

    /**
     * Completion respects demand (every(duration))
     * <p>
     * Main fails with BackPressureFailure: Cannot emit item due to lack of requests
     */
    @Test
    void completionMustNotEmitFinalListWithoutDemandForTimeOnlyGrouping() {
        AssertSubscriber<List<Integer>> sub = subscribe(Integer.MAX_VALUE, 1);

        emit(1, 3);
        fireLatestTimer();
        assertThat(sub.getItems()).containsExactly(List.of(1, 2, 3));

        // Upstream has unbounded demand: more items arrive while downstream demand is 0, then upstream completes
        emit(4, 5);
        emitter.get().complete();

        assertThat(sub.getItems()).hasSize(1);
        sub.assertNotTerminated();

        sub.request(1);
        assertThat(sub.getItems()).containsExactly(List.of(1, 2, 3), List.of(4, 5));
        sub.assertCompleted();
    }

    /**
     * Failure must be propagated immediately and held items must be dropped
     * <p>
     * Main fails with BackPressureFailure: Cannot emit item due to lack of requests
     */
    @Test
    void failureMustBePropagatedImmediatelyAndDropHeldItems() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 1);

        emit(0, 2);
        fireLatestTimer();
        assertThat(sub.getItems()).containsExactly(List.of(0, 1, 2));

        // The next batch times out while downstream has no demand: it is held, not emitted, not failed
        emit(3, 5);
        fireLatestTimer();
        assertThat(sub.getItems()).hasSize(1);
        sub.assertNotTerminated();

        // A failure does not wait for demand, and the held items are dropped
        emitter.get().fail(new IOException("boom"));
        sub.assertFailedWith(IOException.class, "boom");
        assertThat(sub.getItems()).containsExactly(List.of(0, 1, 2));

        // Nothing is emitted after the failure, even when downstream requests more
        sub.request(1);
        assertThat(sub.getItems()).containsExactly(List.of(0, 1, 2));
    }

    /**
     * Upstream demand bounded
     * <p>
     * Main fails with 17L > 10L
     */
    @Test
    void upstreamDemandMustStayBoundedWithPartialFlushes() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 1);

        int emitted = 0;
        for (int round = 0; round < 20; round++) {
            // 3 items, then the timer flushes a partial batch, then downstream asks for the next list
            for (int i = 0; i < 3; i++) {
                emitter.get().emit(emitted++);
            }
            fireLatestTimer();
            sub.request(1);
            // Items requested from upstream but not received yet must not keep growing
            assertThat(upstreamRequested.get() - emitted).isLessThanOrEqualTo(SIZE);
        }
        assertThat(sub.getItems()).hasSize(20);
        assertThat(sub.getFailure()).isNull();
    }

    @Test
    void requestNDrainsMultipleQueuedBatches() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 1);

        emit(0, 2);
        fireLatestTimer();
        assertThat(sub.getItems()).hasSize(1);

        emit(3, 5);
        fireLatestTimer();
        emit(6, 8);
        fireLatestTimer();
        assertThat(sub.getItems()).hasSize(1);
        sub.assertNotTerminated();

        sub.request(3);
        assertThat(sub.getItems()).hasSize(3);
        assertThat(sub.getItems()).containsExactly(List.of(0, 1, 2), List.of(3, 4, 5), List.of(6, 7, 8));

        emit(9, 11);
        fireLatestTimer();
        assertThat(sub.getItems()).hasSize(4);
    }

    @Test
    void requestMoreThanPendingAfterCompletion() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 1);

        emit(0, 2);
        fireLatestTimer();
        assertThat(sub.getItems()).hasSize(1);

        emit(3, 7);
        emitter.get().complete();

        assertThat(sub.getItems()).hasSize(1);
        sub.assertNotTerminated();

        sub.request(5);
        assertThat(sub.getItems()).hasSize(2);
        assertThat(sub.getItems()).containsExactly(List.of(0, 1, 2), List.of(3, 4, 5, 6, 7));
        sub.assertCompleted();
    }

    @Test
    void cancellationDropsPendingBatches() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 1);

        emit(0, 2);
        fireLatestTimer();
        assertThat(sub.getItems()).hasSize(1);

        emit(3, 5);
        fireLatestTimer();
        assertThat(sub.getItems()).hasSize(1);

        sub.cancel();
        sub.request(1);
        assertThat(sub.getItems()).hasSize(1);
        sub.assertNotTerminated();
    }

    @Test
    void emitEmptyListIfNoItemDrainsAccumulatedEmptyLists() {
        Multi<List<Integer>> multi = new MultiBufferWithTimeoutOp<>(
                Multi.createFrom().<Integer> emitter(e -> {
                }),
                10, Duration.ofHours(1), scheduler, true);

        AssertSubscriber<List<Integer>> sub = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        for (int i = 0; i < 5; i++) {
            scheduler.runTask(i);
        }
        assertThat(sub.getItems()).hasSize(1);
        assertThat(sub.getItems().get(0)).isEmpty();

        sub.request(4);
        assertThat(sub.getItems()).hasSize(5);
        for (List<Integer> list : sub.getItems()) {
            assertThat(list).isEmpty();
        }

        sub.request(1);
        scheduler.runTask(5);
        assertThat(sub.getItems()).hasSize(6);
    }

    @RepeatedTest(500)
    void concurrencySmokeTestWithBoundedDemand() {
        ScheduledThreadPoolExecutor realScheduler = new ScheduledThreadPoolExecutor(2);
        try {
            AtomicReference<MultiEmitter<? super Integer>> emRef = new AtomicReference<>();
            Multi<Integer> upstream = Multi.createFrom().<Integer> emitter(emRef::set);

            AssertSubscriber<List<Integer>> sub = new MultiBufferWithTimeoutOp<>(upstream, 5,
                    Duration.ofMillis(1), realScheduler, false)
                    .subscribe().withSubscriber(AssertSubscriber.create(0));

            for (int i = 0; i < 50; i++) {
                emRef.get().emit(i);
            }
            emRef.get().complete();

            for (int i = 0; i < 5000 && !sub.hasCompleted(); i++) {
                sub.request(1);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            sub.awaitCompletion(Duration.ofSeconds(5));
            assertThat(sub.getFailure()).isNull();
            int totalItems = sub.getItems().stream().mapToInt(List::size).sum();
            assertThat(totalItems).isEqualTo(50);
        } finally {
            realScheduler.shutdownNow();
        }
    }

    @Test
    void requestMaxValueAfterBoundedRequest() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 1);

        emit(0, 2);
        fireLatestTimer();
        assertThat(sub.getItems()).hasSize(1);

        sub.request(Long.MAX_VALUE);

        emit(3, 32);
        assertThat(sub.getItems()).hasSize(4);
        for (List<Integer> list : sub.getItems()) {
            assertThat(list.size()).isLessThanOrEqualTo(SIZE);
        }

        emitter.get().complete();
        sub.assertCompleted();
        int totalItems = sub.getItems().stream().mapToInt(List::size).sum();
        assertThat(totalItems).isEqualTo(33);
    }

    @Test
    void upstreamDemandBoundedWithRequestN() {
        AssertSubscriber<List<Integer>> sub = subscribe(SIZE, 2);

        int emitted = 0;
        for (int round = 0; round < 10; round++) {
            for (int i = 0; i < 3; i++) {
                emitter.get().emit(emitted++);
            }
            fireLatestTimer();
            sub.request(2);
            assertThat(upstreamRequested.get() - emitted).isLessThanOrEqualTo(SIZE);
        }
        assertThat(sub.getFailure()).isNull();
    }
}
