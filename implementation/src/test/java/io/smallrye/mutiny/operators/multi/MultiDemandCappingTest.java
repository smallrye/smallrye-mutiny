package io.smallrye.mutiny.operators.multi;

import static java.lang.Math.max;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

class MultiDemandCappingTest {

    @Test
    void capToConstantAndCheckDemandAccumulation() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .capDemandsTo(25)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(Long.MAX_VALUE);
        sub.assertNotTerminated();
        assertThat(sub.getItems()).hasSize(25).contains(10, 15).doesNotContain(25, 30);

        sub.request(Long.MAX_VALUE);
        sub.assertNotTerminated();
        assertThat(sub.getItems()).hasSize(50).contains(25, 30).doesNotContain(75);

        sub.request(1L);
        sub.assertNotTerminated();
        assertThat(sub.getItems()).hasSize(75);

        sub.request(2L);
        sub.assertCompleted();
        assertThat(sub.getItems()).hasSize(100);
    }

    @Test
    void capWithHalfFunction() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .capDemandsUsing(requested -> max(1L, requested / 2L))
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(50L);
        sub.assertNotTerminated();
        assertThat(sub.getItems()).hasSize(25);
    }

    @ParameterizedTest
    @ValueSource(longs = { -1L, 0L })
    void rejectBadCapConstant(long max) {
        assertThatThrownBy(() -> Multi.createFrom().range(0, 100).capDemandsTo(max))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be greater than zero");
    }

    @Test
    void rejectNullFunction() {
        assertThatThrownBy(() -> Multi.createFrom().range(0, 100).capDemandsUsing(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be `null`");
    }

    @Test
    void functionMustDemandLessThanRequested() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .capDemandsUsing(requested -> 2L * requested)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(10L);
        sub.assertFailedWith(IllegalStateException.class,
                "The demand capping function computed a request of 20 elements while the outstanding demand is of 10 elements");
    }

    @Test
    void operatorMustRejectNegativeRequests() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .capDemandsUsing(requested -> requested)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(-1L);
        sub.assertFailedWith(IllegalArgumentException.class, "must be greater than 0");
    }

    @Test
    void functionMustDemandPositiveRequests() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .capDemandsUsing(requested -> -1L)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(10L);
        sub.assertFailedWith(IllegalArgumentException.class, "must be greater than 0");
    }

    @Test
    void functionMustNotReturnNull() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .capDemandsUsing(requested -> null)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(10L);
        sub.assertFailedWith(IllegalArgumentException.class, "`actualDemand` must not be `null`");
    }

    @Test
    void functionMustNotThrow() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(0, 100)
                .capDemandsUsing(requested -> {
                    throw new RuntimeException("boom");
                })
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(10L);
        sub.assertFailedWith(RuntimeException.class, "boom");
    }

    @ParameterizedTest
    @MethodSource("failingRequests")
    void failuresMustCancelUpstream(LongFunction<Long> function, long request, Class<? extends Throwable> failure) {
        AtomicBoolean cancelled = new AtomicBoolean();
        AssertSubscriber<Integer> sub = Multi.createFrom().<Integer> nothing()
                .onCancellation().invoke(() -> cancelled.set(true))
                .capDemandsUsing(function)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(request);
        sub.assertFailedWith(failure);
        assertThat(cancelled).isTrue();
    }

    static Stream<Arguments> failingRequests() {
        return Stream.of(
                Arguments.of((LongFunction<Long>) requested -> requested, -1L, IllegalArgumentException.class),
                Arguments.of((LongFunction<Long>) requested -> -1L, 10L, IllegalArgumentException.class),
                Arguments.of((LongFunction<Long>) requested -> 2L * requested, 10L, IllegalStateException.class),
                Arguments.of((LongFunction<Long>) requested -> {
                    throw new RuntimeException("boom");
                }, 10L, RuntimeException.class));
    }
}
