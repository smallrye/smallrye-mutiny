package snippets;

import io.smallrye.mutiny.Uni;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheTest {

    @Test
    public void indefinitely() {
        // tag::indefinitely[]
        AtomicInteger source = new AtomicInteger(0);
        Uni<Integer> uni = Uni.createFrom().item(source::getAndIncrement);
        // On first subscription to this `cachedUni` the item or failure will be computed from `uni`
        Uni<Integer> cachedUni = uni.cacheEvents().indefinitely();
        cachedUni.subscribe().with(System.out::println); // prints '0'
        // further subscriptions will not cause re-subscription to the upstream `uni` but retrieve the cached item
        cachedUni.subscribe().with(System.out::println); // prints again '0'
        // end::indefinitely[]
        assertThat(cachedUni.await().indefinitely()).isEqualTo(0);
    }

    @Test
    public void whilst() {
        // tag::whilst[]
        Uni<Instant> uni = Uni.createFrom().item(Instant::now);
        // After the item was retrieved from `uni` the `cachedUni` will emit the cached events until the predicate fails.
        // As soon as an event does not pass the predicate, the `cachedUni` will re-subscribe to the upstream for computation of a new event.
        Uni<Instant> cachedUni = uni.cacheEvents().whilst((item, failure) -> item.isAfter(Instant.now().minus(Duration.ofSeconds(10))));
        cachedUni.subscribe().with(System.out::println); // prints '2020-08-05T23:42:05.1337Z'
        cachedUni.subscribe().with(System.out::println); // prints again '2020-08-05T23:42:05.1337Z'
        // end::whilst[]
        assertThat(cachedUni.await().indefinitely()).isEqualTo(cachedUni.await().indefinitely());
    }

    @Test
    public void others() {
        Uni<Integer> uni = Uni.createFrom().item(0);
        // tag::others[]
        // Just an item will be cached (indefinitely), a failure will cause a re-subscription on next subscriber to the `cachedItemUni`.
        Uni<Integer> cachedItemUni = uni.cacheEvents().itemOnly();

        // A cached item will be invalidated the given duration after it was retrieved. Failures always cause a re-subscription to the upstream.
        Uni<Integer> durationCacheUni = uni.cacheEvents().atMost(Duration.ofSeconds(30));
        // end::others[]
        assertThat(cachedItemUni.await().indefinitely()).isEqualTo(0);
        assertThat(durationCacheUni.await().indefinitely()).isEqualTo(0);
    }

}
