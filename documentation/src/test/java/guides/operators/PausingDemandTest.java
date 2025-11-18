package guides.operators;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.DemandPauser;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class PausingDemandTest {

    @Test
    void basicPausing() {
        // <basic>
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Integer> sub = AssertSubscriber.create();
        Multi.createFrom().range(0, 100)
                .pauseDemand().using(pauser)
                // Throttle the multi
                .onItem().call(i -> Uni.createFrom().nullItem()
                        .onItem().delayIt().by(Duration.ofMillis(10)))
                .subscribe().withSubscriber(sub);

        // Unbounded request
        sub.request(Long.MAX_VALUE);
        // Wait for some items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(10));

        // Pause the stream
        pauser.pause();
        assertThat(pauser.isPaused()).isTrue();

        int sizeWhenPaused = sub.getItems().size();

        // Wait - no new items should arrive (except a few in-flight)
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);
        assertThat(sub.getItems()).hasSizeLessThanOrEqualTo(sizeWhenPaused + 5);

        // Resume the stream
        pauser.resume();
        assertThat(pauser.isPaused()).isFalse();

        // All items eventually arrive
        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(100);
        // </basic>
    }

    @Test
    void initiallyPaused() {
        // <initially-paused>
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 50)
                .pauseDemand()
                .paused(true)  // Start paused
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // No items arrive while paused
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);
        assertThat(sub.getItems()).isEmpty();
        assertThat(pauser.isPaused()).isTrue();

        // Resume to start receiving items
        pauser.resume();
        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(50);
        // </initially-paused>
    }

    @Test
    void lateSubscription() {
        // <late-subscription>
        DemandPauser pauser = new DemandPauser();
        AtomicBoolean subscribed = new AtomicBoolean(false);

        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 50)
                .onSubscription().invoke(() -> subscribed.set(true))
                .pauseDemand()
                .paused(true)           // Start paused
                .lateSubscription(true) // Delay subscription until resumed
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Stream is not subscribed yet
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);
        assertThat(subscribed.get()).isFalse();
        assertThat(sub.getItems()).isEmpty();

        // Resume triggers subscription and item flow
        pauser.resume();
        await().untilAsserted(() -> assertThat(subscribed.get()).isTrue());
        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(50);
        // </late-subscription>
    }

    @Test
    void bufferStrategy() {
        // <buffer-strategy>
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Long> sub = AssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .select().first(100)
                .pauseDemand()
                .bufferStrategy(BackPressureStrategy.BUFFER)
                .bufferSize(20)  // Buffer up to 20 items
                .using(pauser)
                .subscribe().withSubscriber(sub);

        // Wait for some items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(5));

        // Pause - items buffer up to the limit
        pauser.pause();

        // Wait for buffer to fill
        await().pollDelay(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(pauser.bufferSize()).isGreaterThan(0));

        // Buffer size is capped
        assertThat(pauser.bufferSize()).isLessThanOrEqualTo(20);

        // Resume drains the buffer
        pauser.resume();
        await().untilAsserted(() -> assertThat(pauser.bufferSize()).isEqualTo(0));
        // </buffer-strategy>
    }

    @Test
    void dropStrategy() {
        // <drop-strategy>
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Long> sub = Multi.createFrom().ticks().every(Duration.ofMillis(5))
                .select().first(200)
                .pauseDemand()
                .bufferStrategy(BackPressureStrategy.DROP)  // Drop items while paused
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Wait for some items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(20));

        // Pause - subsequent items are dropped
        pauser.pause();
        int sizeWhenPaused = sub.getItems().size();

        // Wait while items are dropped
        await().pollDelay(Duration.ofMillis(200)).until(() -> true);

        // Resume - continue from current position
        pauser.resume();
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(sub.getItems().size()).isGreaterThan(sizeWhenPaused + 20));

        sub.awaitCompletion();
        // Not all items arrived (some were dropped)
        assertThat(sub.getItems()).hasSizeLessThan(200);
        // </drop-strategy>
    }

    @Test
    void bufferManagement() {
        // <buffer-management>
        DemandPauser pauser = new DemandPauser();

        AssertSubscriber<Long> sub = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .select().first(100)
                .pauseDemand()
                .bufferStrategy(BackPressureStrategy.BUFFER)
                .bufferUnconditionally()  // Unbounded buffer
                .using(pauser)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Wait for some items
        await().untilAsserted(() -> assertThat(sub.getItems()).hasSizeGreaterThan(5));

        // Pause and let buffer fill
        pauser.pause();
        await().pollDelay(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(pauser.bufferSize()).isGreaterThan(0));

        int bufferSize = pauser.bufferSize();
        assertThat(bufferSize).isGreaterThan(0);

        // Clear the buffer
        boolean cleared = pauser.clearBuffer();
        assertThat(cleared).isTrue();
        assertThat(pauser.bufferSize()).isEqualTo(0);

        // Resume - items continue from current position (cleared items are lost)
        pauser.resume();
        // </buffer-management>
    }

    @Test
    void externalControl() {
        // <external-control>
        DemandPauser pauser = new DemandPauser();

        // Create and transform the stream
        Multi<String> stream = Multi.createFrom().range(0, 1000)
                .pauseDemand().using(pauser)
                .onItem().transform(i -> "Item-" + i)
                .onItem().transform(String::toUpperCase);

        // Control the stream from anywhere
        pauser.pause();

        // Later, subscribe to the stream
        AssertSubscriber<String> sub = stream
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // No items while paused
        await().pollDelay(Duration.ofMillis(100)).until(() -> true);
        assertThat(sub.getItems()).isEmpty();

        // Resume to receive items
        pauser.resume();
        sub.awaitCompletion();
        assertThat(sub.getItems()).hasSize(1000);
        // </external-control>
    }

    @Test
    void conditionalFlow() {
        // <conditional-flow>
        DemandPauser pauser = new DemandPauser();
        AtomicInteger errorCount = new AtomicInteger(0);

        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .pauseDemand().using(pauser)
                .onItem().invoke(item -> {
                    // Simulate error-prone processing
                    if (item % 20 == 0 && item > 0) {
                        errorCount.incrementAndGet();
                        pauser.pause(); // Pause on errors
                    }
                })
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        // Stream pauses when errors occur
        await().untilAsserted(() -> assertThat(errorCount.get()).isGreaterThan(0));
        await().untilAsserted(() -> assertThat(pauser.isPaused()).isTrue());

        // Handle the error, then resume
        errorCount.set(0);
        pauser.resume();

        // Stream continues
        await().pollDelay(Duration.ofMillis(50)).until(() -> true);
        assertThat(sub.getItems().size()).isGreaterThan(20);
        // </conditional-flow>
    }
}
