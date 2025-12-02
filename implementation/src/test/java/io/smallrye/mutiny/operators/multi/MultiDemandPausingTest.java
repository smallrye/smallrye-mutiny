package io.smallrye.mutiny.operators.multi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.DemandPauser;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiDemandPausingTest {

    @Test
    public void testPauseDemand() {
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .pauseDemand().using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Wait for some items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(10));

        // Pause the stream
        pauser.pause();
        assertThat(pauser.isPaused()).isTrue();

        int sizeWhenPaused = sub.getItems().size();

        // Wait a bit - no new items should arrive
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);
        assertThat(sub.getItems()).hasSizeLessThanOrEqualTo(sizeWhenPaused + 5); // allow for some in-flight

        // Resume the stream
        pauser.resume();
        assertThat(pauser.isPaused()).isFalse();

        // All items should eventually arrive
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSize(100));
    }

    @Test
    public void testDropStrategy() {
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Integer> sub = Multi.createFrom().ticks().every(Duration.ofMillis(5))
                .map(Long::intValue)
                .select().first(200)
                .pauseDemand()
                .bufferStrategy(BackPressureStrategy.DROP)
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Wait for some items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(20));

        // Pause the stream - items will be dropped since we're using DROP strategy
        pauser.pause();
        assertThat(pauser.isPaused()).isTrue();

        int sizeWhenPaused = sub.getItems().size();

        // Wait for items to be dropped (stream is still emitting)
        await().pollDelay(Duration.ofMillis(200)).until(() -> true);

        // Items should not have advanced much (maybe a few in-flight)
        assertThat(sub.getItems().size()).isLessThanOrEqualTo(sizeWhenPaused + 10);

        // Resume the stream
        pauser.resume();

        // Not all items will arrive (some were dropped while paused)
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(sub.getItems().size()).isGreaterThan(sizeWhenPaused + 20));

        // Should still be less than total since some were dropped
        assertThat(sub.getItems()).hasSizeLessThan(200);
    }

    @Test
    public void testInitiallyPaused() {
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 50)
                .pauseDemand()
                .paused(true)
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Wait a bit - no items should arrive
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);
        assertThat(sub.getItems()).isEmpty();
        assertThat(pauser.isPaused()).isTrue();

        // Resume the stream
        pauser.resume();

        // All items should arrive
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSize(50));
    }

    @Test
    public void testBoundedBuffer() {
        DemandPauser pauser = new DemandPauser();

        Multi<Integer> source = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .map(Long::intValue)
                .select().first(100);

        AssertSubscriber<Integer> sub = source.pauseDemand()
                .bufferStrategy(BackPressureStrategy.BUFFER)
                .bufferSize(20)
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Wait for some items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(5));

        // Pause and wait for buffer overflow
        pauser.pause();

        // Buffer should overflow and cause failure
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(sub.getFailure()).isInstanceOf(IllegalStateException.class)
                        .hasMessage("Buffer overflow: cannot buffer more than 20 items"));

        assertThat(sub.getItems()).hasSizeLessThan(100);
    }

    @Test
    public void testBufferSize() {
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Integer> sub = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .map(Long::intValue)
                .select().first(100)
                .pauseDemand()
                .bufferStrategy(BackPressureStrategy.BUFFER)
                .bufferSize(20)
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Wait for some items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(5));

        // Pause - items should buffer
        pauser.pause();

        // Wait for buffer to fill
        await().pollDelay(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(pauser.bufferSize()).isGreaterThan(0));

        assertThat(pauser.bufferSize()).isLessThanOrEqualTo(20);

        // Resume
        pauser.resume();

        await().untilAsserted(() -> assertThat(pauser.bufferSize()).isEqualTo(0));
    }

    @Test
    public void testClearBuffer() {
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Integer> sub = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .map(Long::intValue)
                .select().first(100)
                .pauseDemand()
                .bufferStrategy(BackPressureStrategy.BUFFER)
                .bufferUnconditionally()
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Wait for some items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(5));

        // Pause - items should buffer
        pauser.pause();

        // Wait for buffer to fill
        await().pollDelay(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(pauser.bufferSize()).isGreaterThan(0));

        int bufferSize = pauser.bufferSize();
        assertThat(bufferSize).isGreaterThan(0);

        // Clear the buffer
        boolean cleared = pauser.clearBuffer();
        assertThat(cleared).isTrue();
        assertThat(pauser.bufferSize()).isEqualTo(0);

        // Resume
        pauser.resume();

        // Items will continue from where we resumed, not from buffer
        await().pollDelay(Duration.ofMillis(200)).until(() -> true);
        assertThat(sub.getItems()).hasSizeLessThan(100); // Some items were dropped
    }

    @Test
    public void testLateSubscription() {
        DemandPauser pauser = new DemandPauser();
        AtomicBoolean subscribed = new AtomicBoolean(false);

        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 50)
                .onSubscription().invoke(() -> subscribed.set(true))
                .pauseDemand()
                .paused(true) // Start paused
                .lateSubscription(true) // Delay subscription until resumed
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Stream is not subscribed yet
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);
        assertThat(subscribed.get()).isFalse();
        assertThat(sub.getItems()).isEmpty();
        assertThat(pauser.isPaused()).isTrue();

        // Resume triggers subscription and item flow
        pauser.resume();
        await().untilAsserted(() -> assertThat(subscribed.get()).isTrue());
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSize(50));
        assertThat(pauser.isPaused()).isFalse();
    }

    @Test
    public void testLateSubscriptionWithHotSource() {
        DemandPauser pauser = new DemandPauser();
        AtomicBoolean subscribed = new AtomicBoolean(false);

        // Hot source that emits immediately
        AssertSubscriber<Long> sub = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .onSubscription().invoke(() -> subscribed.set(true))
                .select().first(100)
                .pauseDemand()
                .paused(true) // Start paused
                .lateSubscription(true) // Delay subscription - won't miss early items
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Stream is not subscribed yet, no items are being emitted
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);
        assertThat(subscribed.get()).isFalse();
        assertThat(sub.getItems()).isEmpty();

        // Resume triggers subscription and starts receiving items from the beginning
        pauser.resume();
        await().untilAsserted(() -> assertThat(subscribed.get()).isTrue());

        // First item should be 0, not some later value
        await().untilAsserted(() -> assertThat(sub.getItems()).isNotEmpty());
        assertThat(sub.getItems().get(0)).isEqualTo(0L);

        // Eventually receive all items
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(sub.getItems()).hasSize(100));
    }

    @Test
    public void testLateSubscriptionNotPausedSubscribesImmediately() {
        DemandPauser pauser = new DemandPauser();
        AtomicBoolean subscribed = new AtomicBoolean(false);

        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 50)
                .onSubscription().invoke(() -> subscribed.set(true))
                .pauseDemand()
                .lateSubscription(true) // Late subscription enabled but...
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Stream subscribes immediately because it's not paused
        // lateSubscription only delays when initially paused
        await().untilAsserted(() -> assertThat(subscribed.get()).isTrue());

        // Items flow normally
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSize(50));
        assertThat(pauser.isPaused()).isFalse();
    }

    @Test
    public void testMethodsWithoutSubscription() {
        DemandPauser pauser = new DemandPauser();

        // Pauser is bound but stream not subscribed yet
        Multi.createFrom().range(0, 50)
                .pauseDemand()
                .using(pauser);

        // Pauser is bound
        assertThat(pauser.isBound()).isTrue();

        // Can check state even without subscription
        assertThat(pauser.isPaused()).isFalse();

        // Can pause before subscription
        pauser.pause();
        assertThat(pauser.isPaused()).isTrue();

        // Buffer operations work (return defaults when no subscription)
        assertThat(pauser.bufferSize()).isEqualTo(0);
        assertThat(pauser.clearBuffer()).isFalse(); // Returns false when not subscribed

        // Resume also works
        pauser.resume();
        assertThat(pauser.isPaused()).isFalse();
    }

    @Test
    public void testPauserNotBound() {
        DemandPauser pauser = new DemandPauser();

        // Pauser is not bound yet
        assertThat(pauser.isBound()).isFalse();

        // All operations should throw IllegalStateException
        assertThatThrownBy(() -> pauser.pause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DemandPauser is not bound");

        assertThatThrownBy(() -> pauser.resume())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DemandPauser is not bound");

        assertThatThrownBy(() -> pauser.isPaused())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DemandPauser is not bound");

        assertThatThrownBy(() -> pauser.bufferSize())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DemandPauser is not bound");

        assertThatThrownBy(() -> pauser.clearBuffer())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DemandPauser is not bound");
    }

    @Test
    public void testInvalidBufferStrategy() {
        DemandPauser pauser = new DemandPauser();

        // ERROR strategy is not supported for pauseDemand
        assertThatThrownBy(() -> Multi.createFrom().range(0, 50)
                .pauseDemand()
                .bufferStrategy(BackPressureStrategy.ERROR)
                .using(pauser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Demand pauser only supports BUFFER, DROP or IGNORE strategy");

        // LATEST strategy is not supported for pauseDemand
        assertThatThrownBy(() -> Multi.createFrom().range(0, 50)
                .pauseDemand()
                .bufferStrategy(BackPressureStrategy.LATEST)
                .using(pauser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Demand pauser only supports BUFFER, DROP or IGNORE strategy");
    }

    @Test
    public void testIgnoreStrategy() {
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Integer> sub = Multi.createFrom().ticks().every(Duration.ofMillis(5))
                .map(Long::intValue)
                .select().first(100)
                .pauseDemand()
                .bufferStrategy(BackPressureStrategy.IGNORE)
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Wait for some items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(20));

        // Pause the stream - already requested items continue to flow with IGNORE strategy
        pauser.pause();
        assertThat(pauser.isPaused()).isTrue();

        int sizeWhenPaused = sub.getItems().size();

        // With IGNORE strategy, already-requested items continue to arrive even when paused
        // No new demand is issued, but in-flight items keep flowing
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);

        // Items may continue to arrive (in-flight items) even when paused
        int sizeAfterPause = sub.getItems().size();
        // Some items may have arrived during pause with IGNORE strategy
        assertThat(sizeAfterPause).isGreaterThanOrEqualTo(sizeWhenPaused);

        // Resume the stream - new demand will be issued
        pauser.resume();
        assertThat(pauser.isPaused()).isFalse();

        // All items should eventually arrive
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(sub.getItems()).hasSize(100));

        // Buffer should always be 0 with IGNORE strategy (no buffering)
        assertThat(pauser.bufferSize()).isEqualTo(0);
    }

    @Test
    public void testCancelWhilePaused() {
        DemandPauser pauser = new DemandPauser();
        AtomicBoolean cancelled = new AtomicBoolean(false);

        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .onCancellation().invoke(() -> cancelled.set(true))
                .pauseDemand().using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create());

        // Request and wait for some items
        sub.request(10);
        assertThat(sub.getItems()).hasSize(10);

        // Pause the stream
        pauser.pause();
        assertThat(pauser.isPaused()).isTrue();

        int sizeWhenPaused = sub.getItems().size();

        // Cancel while paused
        sub.cancel();

        await().untilAsserted(() -> assertThat(cancelled.get()).isTrue());

        // Resume after cancel
        pauser.resume();

        // No new items should arrive after cancel
        int sizeAfterCancel = sub.getItems().size();
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);
        assertThat(sizeWhenPaused).isEqualTo(sizeAfterCancel);
    }

    @Test
    public void testCancelWithBufferedItems() {
        DemandPauser pauser = new DemandPauser();
        List<Long> items = new CopyOnWriteArrayList<>();
        AtomicBoolean cancelled = new AtomicBoolean(false);

        Multi<Long> source = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .onCancellation().invoke(() -> cancelled.set(true))
                .select().first(100);

        source.pauseDemand()
                .bufferStrategy(BackPressureStrategy.BUFFER)
                .bufferUnconditionally()
                .using(pauser)
                .subscribe().withSubscriber(new MultiSubscriber<Long>() {
                    private Flow.Subscription subscription;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onItem(Long item) {
                        items.add(item);
                        if (items.size() > 5) {
                            pauser.pause();
                            // Wait for buffer to fill
                            await().pollDelay(Duration.ofMillis(100)).until(() -> true);
                            // Cancel with items in buffer
                            subscription.cancel();
                        }
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                    }

                    @Override
                    public void onCompletion() {
                    }
                });

        await().untilAsserted(() -> assertThat(cancelled.get()).isTrue());

        // Buffer should have been cleared on cancel
        assertThat(pauser.bufferSize()).isEqualTo(0);

        // No new items should arrive after cancel
        int sizeAfterCancel = items.size();
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);
        assertThat(items.size()).isEqualTo(sizeAfterCancel);
    }

    @Test
    public void testOnFailureWhilePaused() {
        DemandPauser pauser = new DemandPauser();
        List<Integer> items = new CopyOnWriteArrayList<>();
        AtomicInteger failureCount = new AtomicInteger();
        AtomicBoolean wasFailure = new AtomicBoolean(false);

        Multi<Integer> multi = Multi.createFrom().emitter(emitter -> {
            for (int i = 0; i < 20; i++) {
                emitter.emit(i);
            }
            emitter.fail(new RuntimeException("Test failure"));
        });
        multi
                .pauseDemand().using(pauser)
                .subscribe().with(
                        item -> {
                            items.add(item);
                            if (items.size() > 5) {
                                pauser.pause();
                            }
                        },
                        failure -> {
                            wasFailure.set(true);
                            failureCount.incrementAndGet();
                        });

        // Wait for failure to be propagated
        await().untilAsserted(() -> assertThat(wasFailure.get()).isTrue());
        assertThat(failureCount.get()).isEqualTo(1);

        // No new items should arrive after failure
        int sizeAfterFailure = items.size();
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);
        assertThat(items.size()).isEqualTo(sizeAfterFailure);
    }

    @Test
    public void testOnFailureWithBufferedItems() {
        DemandPauser pauser = new DemandPauser();
        List<Integer> items = new CopyOnWriteArrayList<>();
        AtomicInteger failureCount = new AtomicInteger();
        AtomicBoolean wasFailure = new AtomicBoolean(false);

        Multi<Integer> multi = Multi.createFrom().emitter(emitter -> {
            for (int i = 0; i < 100; i++) {
                emitter.emit(i);
                if (i == 50) {
                    emitter.fail(new RuntimeException("Test failure at 50"));
                    return;
                }
            }
        });
        multi
                .pauseDemand()
                .bufferStrategy(BackPressureStrategy.BUFFER)
                .bufferUnconditionally()
                .using(pauser)
                .subscribe().with(
                        item -> {
                            items.add(item);
                            if (items.size() > 5) {
                                pauser.pause();
                            }
                        },
                        failure -> {
                            wasFailure.set(true);
                            failureCount.incrementAndGet();
                        });

        // Wait for failure to be propagated
        await().untilAsserted(() -> assertThat(wasFailure.get()).isTrue());
        assertThat(failureCount.get()).isEqualTo(1);

        // Buffer should be cleared after failure
        assertThat(pauser.bufferSize()).isEqualTo(0);

        // Items received should be less than 51 (some may be buffered when failure occurs)
        assertThat(items.size()).isLessThanOrEqualTo(51);
    }

    @Test
    public void testUpstreamFailurePropagation() {
        DemandPauser pauser = new DemandPauser();
        List<Integer> items = new CopyOnWriteArrayList<>();
        AtomicInteger failureCount = new AtomicInteger();
        RuntimeException expectedException = new RuntimeException("Upstream failure");

        Multi.createFrom().range(0, 10)
                .map(i -> {
                    if (i == 5) {
                        throw expectedException;
                    }
                    return i;
                })
                .pauseDemand().using(pauser)
                .subscribe().with(
                        items::add,
                        failure -> {
                            assertThat(failure).isEqualTo(expectedException);
                            failureCount.incrementAndGet();
                        });

        // Wait for failure to be propagated
        await().untilAsserted(() -> assertThat(failureCount.get()).isEqualTo(1));

        // Should have received items 0-4
        assertThat(items).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    public void testResumeWhenAlreadyResumed() {
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Long> sub = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .select().first(100)
                .pauseDemand().using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Initially not paused
        assertThat(pauser.isPaused()).isFalse();

        // Call resume when already resumed (should be idempotent)
        pauser.resume();
        assertThat(pauser.isPaused()).isFalse();

        // Items should flow normally
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSize(100));

        // Resume again after completion
        pauser.resume();
        assertThat(pauser.isPaused()).isFalse();

        pauser.clearBuffer();
    }

    @Test
    public void testPauseWhenAlreadyPaused() {
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Long> sub = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .select().first(100)
                .pauseDemand().using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Wait for some items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(10));

        // Pause the stream
        pauser.pause();
        assertThat(pauser.isPaused()).isTrue();

        int sizeWhenPaused = sub.getItems().size();

        // Pause again when already paused (should be idempotent)
        pauser.pause();
        assertThat(pauser.isPaused()).isTrue();

        // Wait a bit - no new items should arrive
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);
        assertThat(sub.getItems()).hasSizeLessThanOrEqualTo(sizeWhenPaused + 5); // allow for some in-flight

        // Resume
        pauser.resume();
        assertThat(pauser.isPaused()).isFalse();

        // All items should eventually arrive
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSize(100));
    }

    @Test
    public void testMultiplePauseResumeCycles() {
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .pauseDemand().using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Wait for some items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(10));

        // First pause-resume cycle
        pauser.pause();
        assertThat(pauser.isPaused()).isTrue();
        await().pollDelay(Duration.ofMillis(50)).until(() -> true);
        pauser.resume();
        assertThat(pauser.isPaused()).isFalse();

        // Wait for more items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(30));

        // Second pause-resume cycle
        pauser.pause();
        assertThat(pauser.isPaused()).isTrue();
        await().pollDelay(Duration.ofMillis(50)).until(() -> true);
        pauser.resume();
        assertThat(pauser.isPaused()).isFalse();

        // Wait for more items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(50));

        // Third pause-resume cycle
        pauser.pause();
        assertThat(pauser.isPaused()).isTrue();
        await().pollDelay(Duration.ofMillis(50)).until(() -> true);
        pauser.resume();
        assertThat(pauser.isPaused()).isFalse();

        // All items should eventually arrive
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSize(100));
    }

    @Test
    public void testConcurrentResumeAndCancel() {
        DemandPauser pauser = new DemandPauser();

        // Slow consumer that adds delay to each item processing
        AssertSubscriber<Integer> sub = Multi.createFrom().ticks().every(Duration.ofMillis(1))
                .map(Long::intValue)
                .select().first(1000)
                .pauseDemand()
                .bufferStrategy(BackPressureStrategy.BUFFER)
                .bufferUnconditionally()
                .using(pauser)
                .invoke(ignored -> {
                    try {
                        // Slow consumer - 10ms per item to keep drain loop busy
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Wait for some items to arrive
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(10));

        // Pause to let buffer fill up with lots of items
        pauser.pause();
        await().pollDelay(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(pauser.bufferSize()).isGreaterThan(100));

        int bufferSize = pauser.bufferSize();
        assertThat(bufferSize).isGreaterThan(100);

        // Resume and clear at the same time from different threads
        // This reproduces the race condition: drain loop starts and clearQueue is called concurrently
        Thread resumeThread = new Thread(() -> pauser.resume());
        Thread clearThread = new Thread(() -> sub.cancel());

        resumeThread.start();
        clearThread.start();

        // Wait for both operations to complete
        await().untilAsserted(() -> {
            assertThat(resumeThread.isAlive()).isFalse();
            assertThat(clearThread.isAlive()).isFalse();
        });

        // No concurrent modification exception should occur
        assertThat(sub.getFailure()).isNull();

        // Buffer should be cleared
        await().untilAsserted(() -> assertThat(pauser.bufferSize()).isEqualTo(0));

        // Stream should continue to work normally
        await().pollDelay(Duration.ofMillis(200)).until(() -> true);
        assertThat(sub.getItems()).hasSizeGreaterThan(0);
    }

}
