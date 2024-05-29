package io.smallrye.mutiny.nodeps;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.subscription.BackPressureStrategy;

public class ShadingIT {

    @Test
    public void check_shaded_classes() throws ClassNotFoundException {
        Class.forName("io.smallrye.mutiny.shaded.org.jctools.queues.BaseLinkedQueue");
        Class.forName("io.smallrye.mutiny.shaded.org.jctools.queues.atomic.unpadded.BaseLinkedAtomicUnpaddedQueue");
        Class.forName("io.smallrye.mutiny.shaded.io.smallrye.common.annotation.CheckReturnValue");
    }

    @Test
    public void mpsc_queue_factory() {
        Queue<String> queue = Queues.createMpscArrayQueue(256);

        queue.add("foo");
        queue.add("bar");
        assertEquals("foo", queue.poll());
        assertEquals("bar", queue.poll());
        assertNull(queue.poll());

        assertTrue(queue.getClass().getCanonicalName().contains("shaded"));
    }

    @Test
    public void spsc_queue_factory() {
        Queue<String> queue = Queues.createSpscArrayQueue(256);

        queue.add("foo");
        queue.add("bar");
        assertEquals("foo", queue.poll());
        assertEquals("bar", queue.poll());
        assertNull(queue.poll());

        assertTrue(queue.getClass().getCanonicalName().contains("shaded"));
    }

    @Test
    public void multi_emitter() {
        Multi<Integer> multi = Multi.createFrom().emitter(emitter -> {
            for (int i = 0; i < 100; i++) {
                emitter.emit(i);
            }
            emitter.complete();
        }, BackPressureStrategy.BUFFER);

        List<Integer> suite = multi.collect().asList().await().atMost(Duration.ofSeconds(5));
        assertEquals(100, suite.size());
        assertEquals(0, suite.get(0));
        assertEquals(99, suite.get(99));
    }
}
