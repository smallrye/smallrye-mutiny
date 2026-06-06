package io.smallrye.mutiny.helpers.test;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;

/**
 * A declarative step verifier for {@link Multi} streams.
 * <p>
 * Build a sequence of expectations, then call {@link #verify()} to subscribe and validate each step:
 *
 * <pre>
 * AssertMulti.create(multi)
 *         .expectNext(1, 2, 3)
 *         .expectNextMatches(i -&gt; i &gt; 3, "greater than 3")
 *         .expectComplete()
 *         .verify();
 * </pre>
 *
 * @param <T> the type of items emitted by the Multi
 */
@Experimental("This is an experimental API in Mutiny 3.2.0")
public final class AssertMulti<T> {

    private final Multi<T> multi;
    private final Context context;
    private final List<Step<T>> steps = new ArrayList<>();
    private boolean frozen = false;

    private AssertMulti(Multi<T> multi, Context context) {
        this.multi = nonNull(multi, "multi");
        this.context = nonNull(context, "context");
    }

    /**
     * Create a new {@link AssertMulti} for the given {@link Multi}.
     *
     * @param multi the Multi to verify, must not be {@code null}
     * @param <T> the item type
     * @return a new AssertMulti
     */
    public static <T> AssertMulti<T> create(Multi<T> multi) {
        return new AssertMulti<>(multi, Context.empty());
    }

    /**
     * Create a new {@link AssertMulti} for the given {@link Multi} with a subscription {@link Context}.
     *
     * @param multi the Multi to verify, must not be {@code null}
     * @param context the context, must not be {@code null}
     * @param <T> the item type
     * @return a new AssertMulti
     */
    public static <T> AssertMulti<T> create(Multi<T> multi, Context context) {
        return new AssertMulti<>(multi, context);
    }

    // ---- Item expectations ----

    /**
     * Expect the next item to be equal to {@code expected}.
     *
     * @param expected the expected item
     * @return this AssertMulti
     */
    public AssertMulti<T> expectNext(T expected) {
        return addStep(new Step.ExpectNext<>(List.of(expected)));
    }

    /**
     * Expect the next items to be equal to {@code expected} in order.
     *
     * @param expected the expected items
     * @return this AssertMulti
     */
    @SafeVarargs
    public final AssertMulti<T> expectNext(T... expected) {
        return addStep(new Step.ExpectNext<>(List.of(expected)));
    }

    /**
     * Expect the next item to match the given predicate.
     *
     * @param predicate the predicate to test, must not be {@code null}
     * @param description a description for error messages
     * @return this AssertMulti
     */
    public AssertMulti<T> expectNextMatches(Predicate<? super T> predicate, String description) {
        nonNull(predicate, "predicate");
        nonNull(description, "description");
        return addStep(new Step.ExpectNextMatches<>(predicate, description));
    }

    /**
     * Expect that the next {@code count} items are received (values are not checked).
     *
     * @param count the number of items to expect, must be positive
     * @return this AssertMulti
     */
    public AssertMulti<T> expectNextCount(int count) {
        positive(count, "count");
        return addStep(new Step.ExpectNextCount<>(count));
    }

    // ---- Consumer-based inspection ----

    /**
     * Consume and inspect the next item.
     * The consumer should throw an {@link AssertionError} if the item is not acceptable.
     *
     * @param consumer the consumer, must not be {@code null}
     * @return this AssertMulti
     */
    public AssertMulti<T> consumeNext(Consumer<? super T> consumer) {
        nonNull(consumer, "consumer");
        return addStep(new Step.ConsumeNext<>(consumer));
    }

    /**
     * Consume and inspect the next {@code count} items as a list.
     * The consumer should throw an {@link AssertionError} if the items are not acceptable.
     *
     * @param count the number of items to collect, must be positive
     * @param consumer the consumer, must not be {@code null}
     * @return this AssertMulti
     */
    public AssertMulti<T> consumeNextItems(int count, Consumer<? super List<T>> consumer) {
        positive(count, "count");
        nonNull(consumer, "consumer");
        return addStep(new Step.ConsumeNextItems<>(count, consumer));
    }

    // ---- Demand control ----

    /**
     * Request {@code n} items from upstream without consuming.
     * Use this for backpressure testing or pre-buffering.
     *
     * @param n the number of items to request, must be positive
     * @return this AssertMulti
     */
    public AssertMulti<T> thenRequest(long n) {
        positive(n, "n");
        return addStep(new Step.ThenRequest<>(n));
    }

    // ---- Terminal expectations ----

    /**
     * Expect the stream to complete.
     *
     * @return this AssertMulti
     */
    public AssertMulti<T> expectComplete() {
        return addStep(new Step.ExpectComplete<>());
    }

    /**
     * Expect the stream to fail with any failure.
     *
     * @return this AssertMulti
     */
    public AssertMulti<T> expectFailure() {
        return addStep(new Step.ExpectFailure<>(null));
    }

    /**
     * Expect the stream to fail with a specific failure type.
     *
     * @param type the expected failure type
     * @return this AssertMulti
     */
    public AssertMulti<T> expectFailure(Class<? extends Throwable> type) {
        nonNull(type, "type");
        return addStep(new Step.ExpectFailure<>(t -> {
            if (!type.isInstance(t)) {
                throw new AssertionError(
                        "Expected failure of type " + type.getName() + " but got " + t.getClass().getName(), t);
            }
        }));
    }

    /**
     * Expect the stream to fail with a specific failure type and message substring.
     *
     * @param type the expected failure type
     * @param messageSubstring a substring expected in the failure message
     * @return this AssertMulti
     */
    public AssertMulti<T> expectFailure(Class<? extends Throwable> type, String messageSubstring) {
        nonNull(type, "type");
        nonNull(messageSubstring, "messageSubstring");
        return addStep(new Step.ExpectFailure<>(t -> {
            if (!type.isInstance(t)) {
                throw new AssertionError(
                        "Expected failure of type " + type.getName() + " but got " + t.getClass().getName(), t);
            }
            if (t.getMessage() == null || !t.getMessage().contains(messageSubstring)) {
                throw new AssertionError(
                        "Expected failure message to contain '" + messageSubstring + "' but was '" + t.getMessage()
                                + "'",
                        t);
            }
        }));
    }

    /**
     * Expect the stream to fail and validate the failure with a consumer.
     *
     * @param validator the failure validator, must not be {@code null}
     * @return this AssertMulti
     */
    public AssertMulti<T> expectFailure(Consumer<Throwable> validator) {
        nonNull(validator, "validator");
        return addStep(new Step.ExpectFailure<>(validator));
    }

    // ---- Cancellation ----

    /**
     * Cancel the subscription at this point in the step sequence.
     *
     * @return this AssertMulti
     */
    public AssertMulti<T> thenCancel() {
        return addStep(new Step.ThenCancel<>());
    }

    // ---- Execution ----

    /**
     * Subscribe to the Multi and execute all steps with the default timeout.
     *
     * @throws AssertionError if any step fails
     */
    public void verify() {
        verify(AssertSubscriber.DEFAULT_TIMEOUT);
    }

    /**
     * Subscribe to the Multi and execute all steps with the given timeout per step.
     *
     * @param timeout the maximum wait time per step, must not be {@code null}
     * @throws AssertionError if any step fails
     */
    public void verify(Duration timeout) {
        nonNull(timeout, "timeout");
        freeze();
        validate();

        AssertSubscriber<T> subscriber = AssertSubscriber.create(context, 0);
        multi.subscribe().withSubscriber(subscriber);

        int itemIndex = 0;
        for (int i = 0; i < steps.size(); i++) {
            Step<T> step = steps.get(i);
            try {
                itemIndex = executeStep(step, subscriber, timeout, itemIndex);
            } catch (AssertionError e) {
                throw new AssertionError("Step " + i + " [" + step.description() + "] failed: " + e.getMessage(), e);
            }
        }
    }

    // ---- Internals ----

    private AssertMulti<T> addStep(Step<T> step) {
        if (frozen) {
            throw new IllegalStateException("Cannot add steps after verify() has been called");
        }
        steps.add(step);
        return this;
    }

    private void freeze() {
        frozen = true;
    }

    private void validate() {
        if (steps.isEmpty()) {
            throw new IllegalStateException("No steps defined");
        }
        Step<T> last = steps.get(steps.size() - 1);
        if (!(last instanceof Step.ExpectComplete) && !(last instanceof Step.ExpectFailure)
                && !(last instanceof Step.ThenCancel)) {
            throw new IllegalStateException(
                    "Last step must be a terminal expectation (expectComplete, expectFailure, or thenCancel)");
        }
    }

    private int executeStep(Step<T> step, AssertSubscriber<T> subscriber, Duration timeout, int itemIndex) {
        if (step instanceof Step.ExpectNext<T> expectNext) {
            List<T> expected = expectNext.expected();
            int targetCount = itemIndex + expected.size();
            subscriber.request(expected.size());
            subscriber.awaitItems(targetCount, timeout);
            List<T> items = subscriber.getItems();
            for (int j = 0; j < expected.size(); j++) {
                T actual = items.get(itemIndex + j);
                T exp = expected.get(j);
                if (!Objects.equals(exp, actual)) {
                    throw new AssertionError(
                            "expected <" + exp + "> but received <" + actual + "> at index " + (itemIndex + j));
                }
            }
            return targetCount;
        } else if (step instanceof Step.ExpectNextMatches<T> expectNextMatches) {
            int targetCount = itemIndex + 1;
            subscriber.request(1);
            subscriber.awaitItems(targetCount, timeout);
            T actual = subscriber.getItems().get(itemIndex);
            if (!expectNextMatches.predicate().test(actual)) {
                throw new AssertionError(
                        "received item <" + actual + "> did not match predicate");
            }
            return targetCount;
        } else if (step instanceof Step.ExpectNextCount<T> expectNextCount) {
            int count = expectNextCount.count();
            int targetCount = itemIndex + count;
            subscriber.request(count);
            subscriber.awaitItems(targetCount, timeout);
            return targetCount;
        } else if (step instanceof Step.ConsumeNext<T> consumeNext) {
            int targetCount = itemIndex + 1;
            subscriber.request(1);
            subscriber.awaitItems(targetCount, timeout);
            T actual = subscriber.getItems().get(itemIndex);
            consumeNext.consumer().accept(actual);
            return targetCount;
        } else if (step instanceof Step.ConsumeNextItems<T> consumeNextItems) {
            int count = consumeNextItems.count();
            int targetCount = itemIndex + count;
            subscriber.request(count);
            subscriber.awaitItems(targetCount, timeout);
            List<T> items = subscriber.getItems().subList(itemIndex, itemIndex + count);
            consumeNextItems.consumer().accept(List.copyOf(items));
            return targetCount;
        } else if (step instanceof Step.ThenRequest<T> thenRequest) {
            subscriber.request(thenRequest.n());
            return itemIndex;
        } else if (step instanceof Step.ThenCancel) {
            subscriber.cancel();
            return itemIndex;
        } else if (step instanceof Step.ExpectComplete) {
            subscriber.awaitCompletion(timeout);
            return itemIndex;
        } else if (step instanceof Step.ExpectFailure<T> expectFailure) {
            Consumer<Throwable> validator = expectFailure.validator();
            if (validator != null) {
                subscriber.awaitFailure(validator, timeout);
            } else {
                subscriber.awaitFailure(timeout);
            }
            return itemIndex;
        } else {
            throw new IllegalStateException("Unknown step type: " + step.getClass().getName());
        }
    }

    // ---- Step types ----

    sealed interface Step<T> {

        String description();

        record ExpectNext<T>(List<T> expected) implements Step<T> {
            @Override
            public String description() {
                return "expectNext: " + expected;
            }
        }

        record ExpectNextMatches<T>(Predicate<? super T> predicate,
                String predicateDescription) implements Step<T> {
            @Override
            public String description() {
                return "expectNextMatches: " + predicateDescription;
            }
        }

        record ExpectNextCount<T>(int count) implements Step<T> {
            @Override
            public String description() {
                return "expectNextCount: " + count;
            }
        }

        record ConsumeNext<T>(Consumer<? super T> consumer) implements Step<T> {
            @Override
            public String description() {
                return "consumeNext";
            }
        }

        record ConsumeNextItems<T>(int count, Consumer<? super List<T>> consumer) implements Step<T> {
            @Override
            public String description() {
                return "consumeNextItems: " + count;
            }
        }

        record ThenRequest<T>(long n) implements Step<T> {
            @Override
            public String description() {
                return "thenRequest: " + n;
            }
        }

        record ThenCancel<T>() implements Step<T> {
            @Override
            public String description() {
                return "thenCancel";
            }
        }

        record ExpectComplete<T>() implements Step<T> {
            @Override
            public String description() {
                return "expectComplete";
            }
        }

        record ExpectFailure<T>(Consumer<Throwable> validator) implements Step<T> {
            @Override
            public String description() {
                return "expectFailure";
            }
        }
    }
}
