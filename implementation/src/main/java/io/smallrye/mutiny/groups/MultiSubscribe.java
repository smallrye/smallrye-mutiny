package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.subscription.Subscribers.NO_ON_FAILURE;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.BlockingIterable;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.CancellableSubscriber;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.Subscribers;

public class MultiSubscribe<T> {

    private final AbstractMulti<T> upstream;

    public MultiSubscribe(AbstractMulti<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Subscribes to the {@link Multi} to get a subscription and then start receiving items (
     * based on the passed requests).
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should
     * only subscribe once to a single {@link Multi}.
     * <p>
     * If the {@link Multi} rejects the subscription attempt or otherwise fails it will fire a {@code failure} event
     * receiving by {@link Subscriber#onError(Throwable)}.
     *
     * @param subscriber the subscriber, must not be {@code null}
     * @param <S> the subscriber type
     * @return the passed subscriber
     */
    public <S extends Subscriber<? super T>> S withSubscriber(S subscriber) {
        Subscriber<? super T> actual = Infrastructure.onMultiSubscription(upstream, subscriber);
        upstream.subscribe(actual);
        return subscriber;
    }

    /**
     * Subscribes to the {@link Multi} to get a subscription and then start receiving items (
     * based on the passed requests).
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link MultiSubscriber}. A {@link MultiSubscriber} should
     * only subscribe once to a single {@link Multi}.
     * <p>
     * If the {@link Multi} rejects the subscription attempt or otherwise fails it will fire a {@code failure} event
     * receiving by {@link MultiSubscriber#onFailure(Throwable)}.
     *
     * @param subscriber the subscriber, must not be {@code null}
     * @param <S> the subscriber type
     * @return the passed subscriber
     */
    public <S extends MultiSubscriber<? super T>> S withSubscriber(S subscriber) {
        upstream.subscribe(subscriber);
        return subscriber;
    }

    /**
     * Subscribes to the {@link Multi} to start receiving the items.
     * <p>
     * This method accepts the following callbacks:
     * <ol>
     * <li>{@code onSubscription} receives the {@link Subscription}, you <strong>must</strong> request items using
     * the {@link Subscription#request(long)} method</li>
     * <li>{@code onItem} receives the requested items if any</li>
     * <li>{@code onFailure} receives the failure if any</li>
     * <li>{@code onComplete} receives the completion event</li>
     * </ol>
     * <p>
     * This method returns a {@link Cancellable} to cancel the subscription.
     *
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should
     * only subscribe once to a single {@link Multi}.
     *
     * @param onSubscription the callback receiving the subscription, must not be {@code null}
     * @param onItem the callback receiving the items, must not be {@code null}
     * @param onFailure the callback receiving the failure, must not be {@code null}
     * @param onComplete the callback receiving the completion event, must not be {@code null}
     * @return the cancellable object to cancel the subscription
     */
    public Cancellable with(
            Consumer<? super Subscription> onSubscription,
            Consumer<? super T> onItem,
            Consumer<? super Throwable> onFailure,
            Runnable onComplete) {
        return with(Context.empty(), onSubscription, onItem, onFailure, onComplete);
    }

    /**
     * Subscribes to the {@link Multi} to start receiving the items.
     * <p>
     * This method accepts the following callbacks:
     * <ol>
     * <li>{@code onSubscription} receives the {@link Subscription}, you <strong>must</strong> request items using
     * the {@link Subscription#request(long)} method</li>
     * <li>{@code onItem} receives the requested items if any</li>
     * <li>{@code onFailure} receives the failure if any</li>
     * <li>{@code onComplete} receives the completion event</li>
     * </ol>
     * <p>
     * This method returns a {@link Cancellable} to cancel the subscription.
     *
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should
     * only subscribe once to a single {@link Multi}.
     *
     * @param context the context, must not be {@code null}
     * @param onSubscription the callback receiving the subscription, must not be {@code null}
     * @param onItem the callback receiving the items, must not be {@code null}
     * @param onFailure the callback receiving the failure, must not be {@code null}
     * @param onComplete the callback receiving the completion event, must not be {@code null}
     * @return the cancellable object to cancel the subscription
     */
    public Cancellable with(
            Context context,
            Consumer<? super Subscription> onSubscription,
            Consumer<? super T> onItem,
            Consumer<? super Throwable> onFailure,
            Runnable onComplete) {
        CancellableSubscriber<? super T> subscriber = Subscribers.from(
                nonNull(context, "context"),
                nonNull(onItem, "onItem"),
                nonNull(onFailure, "onFailure"),
                nonNull(onComplete, "onComplete"),
                nonNull(onSubscription, "onSubscription"));
        return withSubscriber(subscriber);
    }

    /**
     * Subscribes to the {@link Multi} to start receiving the items.
     * <p>
     * This method accepts the following callbacks:
     * <ol>
     * <li>{@code onItem} receives the requested items if any</li>
     * <li>{@code onFailure} receives the failure if any</li>
     * <li>{@code onComplete} receives the completion event</li>
     * </ol>
     * <p>
     * This method returns a {@link Cancellable} to cancel the subscription.
     * <p>
     * <strong>Important:</strong> This method request {@link Long#MAX_VALUE} items.
     *
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should
     * only subscribe once to a single {@link Multi}.
     *
     * @param onItem the callback receiving the items, must not be {@code null}
     * @param onFailure the callback receiving the failure, must not be {@code null}
     * @param onComplete the callback receiving the completion event, must not be {@code null}
     * @return the cancellable object to cancel the subscription
     */
    public Cancellable with(
            Consumer<? super T> onItem,
            Consumer<? super Throwable> onFailure,
            Runnable onComplete) {
        return with(Context.empty(), onItem, onFailure, onComplete);
    }

    /**
     * Subscribes to the {@link Multi} to start receiving the items.
     * <p>
     * This method accepts the following callbacks:
     * <ol>
     * <li>{@code onItem} receives the requested items if any</li>
     * <li>{@code onFailure} receives the failure if any</li>
     * <li>{@code onComplete} receives the completion event</li>
     * </ol>
     * <p>
     * This method returns a {@link Cancellable} to cancel the subscription.
     * <p>
     * <strong>Important:</strong> This method request {@link Long#MAX_VALUE} items.
     *
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should
     * only subscribe once to a single {@link Multi}.
     *
     * @param context the context, must not be {@code null}
     * @param onItem the callback receiving the items, must not be {@code null}
     * @param onFailure the callback receiving the failure, must not be {@code null}
     * @param onComplete the callback receiving the completion event, must not be {@code null}
     * @return the cancellable object to cancel the subscription
     */
    public Cancellable with(
            Context context,
            Consumer<? super T> onItem,
            Consumer<? super Throwable> onFailure,
            Runnable onComplete) {
        nonNull(onItem, "onItem");
        nonNull(onFailure, "onFailure");
        nonNull(onComplete, "onComplete");
        CancellableSubscriber<? super T> subscriber = Subscribers.from(
                nonNull(context, "context"),
                nonNull(onItem, "onItem"),
                nonNull(onFailure, "onFailure"),
                nonNull(onComplete, "onComplete"),
                s -> s.request(Long.MAX_VALUE));
        return withSubscriber(subscriber);
    }

    /**
     * Subscribes to the {@link Multi} to start receiving the items.
     * <p>
     * This method accepts the following callbacks:
     * <ol>
     * <li>{@code onItem} receives the requested items if any</li>
     * <li>{@code onFailure} receives the failure if any</li>
     * </ol>
     * <p>
     * So, you won't be notified on stream completion.
     * <p>
     * This method returns a {@link Cancellable} to cancel the subscription.
     * <p>
     * <strong>Important:</strong> This method request {@link Long#MAX_VALUE} items.
     *
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should
     * only subscribe once to a single {@link Multi}.
     * *
     * <p>
     *
     * @param onItem the callback receiving the items, must not be {@code null}
     * @param onFailure the callback receiving the failure, must not be {@code null}
     * @return the cancellable object to cancel the subscription
     */
    public Cancellable with(
            Consumer<? super T> onItem,
            Consumer<? super Throwable> onFailure) {
        return with(Context.empty(), onItem, onFailure);
    }

    /**
     * Subscribes to the {@link Multi} to start receiving the items.
     * <p>
     * This method accepts the following callbacks:
     * <ol>
     * <li>{@code onItem} receives the requested items if any</li>
     * <li>{@code onFailure} receives the failure if any</li>
     * </ol>
     * <p>
     * So, you won't be notified on stream completion.
     * <p>
     * This method returns a {@link Cancellable} to cancel the subscription.
     * <p>
     * <strong>Important:</strong> This method request {@link Long#MAX_VALUE} items.
     *
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should
     * only subscribe once to a single {@link Multi}.
     * *
     * <p>
     *
     * @param context the context, must not be {@code null}
     * @param onItem the callback receiving the items, must not be {@code null}
     * @param onFailure the callback receiving the failure, must not be {@code null}
     * @return the cancellable object to cancel the subscription
     */
    public Cancellable with(
            Context context,
            Consumer<? super T> onItem,
            Consumer<? super Throwable> onFailure) {
        nonNull(onItem, "onItem");
        nonNull(onFailure, "onFailure");
        CancellableSubscriber<? super T> subscriber = Subscribers.from(
                nonNull(context, "context"),
                nonNull(onItem, "onItem"),
                nonNull(onFailure, "onFailure"),
                null,
                s -> s.request(Long.MAX_VALUE));
        return withSubscriber(subscriber);
    }

    /**
     * Subscribes to the {@link Multi} to start receiving the items.
     * <p>
     * This method receives only the {@code onItem} callback, invoked on each item.
     * So, you won't be notified on stream completion, and on failure the default failure handler is used.
     * <p>
     * This method returns a {@link Cancellable} to cancel the subscription.
     * <p>
     * <strong>Important:</strong> This method request {@link Long#MAX_VALUE} items.
     *
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should
     * only subscribe once to a single {@link Multi}.
     * *
     * <p>
     *
     * @param onItem the callback receiving the items, must not be {@code null}
     * @return the cancellable object to cancel the subscription
     */
    public Cancellable with(Consumer<? super T> onItem) {
        return with(Context.empty(), onItem);
    }

    /**
     * Subscribes to the {@link Multi} to start receiving the items.
     * <p>
     * This method receives only the {@code onItem} callback, invoked on each item.
     * So, you won't be notified on stream completion, and on failure the default failure handler is used.
     * <p>
     * This method returns a {@link Cancellable} to cancel the subscription.
     * <p>
     * <strong>Important:</strong> This method request {@link Long#MAX_VALUE} items.
     *
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should
     * only subscribe once to a single {@link Multi}.
     * *
     * <p>
     *
     * @param context the context, must not be {@code null}
     * @param onItem the callback receiving the items, must not be {@code null}
     * @return the cancellable object to cancel the subscription
     */
    public Cancellable with(Context context, Consumer<? super T> onItem) {
        Consumer<? super T> actual = Infrastructure.decorate(nonNull(onItem, "onItem"));
        CancellableSubscriber<? super T> subscriber = Subscribers.from(
                nonNull(context, "context"),
                actual,
                NO_ON_FAILURE,
                null,
                s -> s.request(Long.MAX_VALUE));
        return withSubscriber(subscriber);
    }

    /**
     * Subscribes to the {@link Multi} to start receiving the items.
     * <p>
     * This method accepts the following callbacks:
     * <ol>
     * <li>{@code onItem} receives the requested items if any</li>
     * <li>{@code onComplete} receives the completion event</li>
     * </ol>
     * <p>
     * So, you won't be notified on failure.
     * <p>
     * This method returns a {@link Cancellable} to cancel the subscription.
     * <p>
     * <strong>Important:</strong> This method request {@link Long#MAX_VALUE} items.
     *
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should
     * only subscribe once to a single {@link Multi}.
     *
     * @param onItem the callback receiving the items, must not be {@code null}
     * @param onComplete the callback receiving the completion event, must not be {@code null}
     * @return the cancellable object to cancel the subscription
     */
    public Cancellable with(
            Consumer<? super T> onItem,
            Runnable onComplete) {
        return with(Context.empty(), onItem, onComplete);
    }

    /**
     * Subscribes to the {@link Multi} to start receiving the items.
     * <p>
     * This method accepts the following callbacks:
     * <ol>
     * <li>{@code onItem} receives the requested items if any</li>
     * <li>{@code onComplete} receives the completion event</li>
     * </ol>
     * <p>
     * So, you won't be notified on failure.
     * <p>
     * This method returns a {@link Cancellable} to cancel the subscription.
     * <p>
     * <strong>Important:</strong> This method request {@link Long#MAX_VALUE} items.
     *
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}.
     * Each {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should
     * only subscribe once to a single {@link Multi}.
     *
     * @param context the context, must not be {@code null}
     * @param onItem the callback receiving the items, must not be {@code null}
     * @param onComplete the callback receiving the completion event, must not be {@code null}
     * @return the cancellable object to cancel the subscription
     */
    public Cancellable with(
            Context context,
            Consumer<? super T> onItem,
            Runnable onComplete) {
        nonNull(onItem, "onItem");
        nonNull(onComplete, "onComplete");
        CancellableSubscriber<? super T> subscriber = Subscribers.from(
                nonNull(context, "context"),
                nonNull(onItem, "onItem"),
                null,
                onComplete,
                s -> s.request(Long.MAX_VALUE));
        return withSubscriber(subscriber);
    }

    /**
     * @return a blocking iterable used to consume the items emitted by the upstream {@link Multi}.
     */
    @CheckReturnValue
    public BlockingIterable<T> asIterable() {
        return asIterable(256, () -> new ArrayBlockingQueue<>(256));
    }

    /**
     * @param contextSupplier the context supplier, must not be {@code null}, must not return {@code null}
     * @return a blocking iterable used to consume the items emitted by the upstream {@link Multi}.
     */
    @CheckReturnValue
    public BlockingIterable<T> asIterable(Supplier<Context> contextSupplier) {
        return asIterable(contextSupplier, 256, () -> new ArrayBlockingQueue<>(256));
    }

    /**
     * Consumes the upstream {@link Multi} as an iterable.
     *
     * @param batchSize the number of elements stored in the queue
     * @param supplier the supplier of queue used internally, must not be {@code null}, must not return {@code null}
     * @return a blocking iterable used to consume the items emitted by the upstream {@link Multi}.
     */
    @CheckReturnValue
    public BlockingIterable<T> asIterable(int batchSize, Supplier<Queue<T>> supplier) {
        return asIterable(Context::empty, batchSize, supplier);
    }

    /**
     * Consumes the upstream {@link Multi} as an iterable.
     *
     * @param contextSupplier the context supplier, must not be {@code null}, must not return {@code null}
     * @param batchSize the number of elements stored in the queue
     * @param queueSupplier the supplier of queue used internally, must not be {@code null}, must not return {@code null}
     * @return a blocking iterable used to consume the items emitted by the upstream {@link Multi}.
     */
    @CheckReturnValue
    public BlockingIterable<T> asIterable(Supplier<Context> contextSupplier, int batchSize, Supplier<Queue<T>> queueSupplier) {
        // No interception of the queue supplier.
        return new BlockingIterable<>(upstream, batchSize, queueSupplier, contextSupplier);
    }

    /**
     * @return a <strong>blocking</strong> stream to consume the items from the upstream {@link Multi}.
     */
    @CheckReturnValue
    public Stream<T> asStream() {
        return asStream(256, () -> new ArrayBlockingQueue<>(256));
    }

    /**
     * @param contextSupplier the context supplier, must not be {@code null}, must not return {@code null}
     * @return a <strong>blocking</strong> stream to consume the items from the upstream {@link Multi}.
     */
    @CheckReturnValue
    public Stream<T> asStream(Supplier<Context> contextSupplier) {
        return asStream(contextSupplier, 256, () -> new ArrayBlockingQueue<>(256));
    }

    /**
     * Consumes the items from the upstream {@link Multi} as a blocking stream.
     *
     * @param batchSize the number of element stored in the queue
     * @param supplier the supplier of queue used internally, must not be {@code null}, must not return {@code null}
     * @return a blocking stream used to consume the items from {@link Multi}
     */
    @CheckReturnValue
    public Stream<T> asStream(int batchSize, Supplier<Queue<T>> supplier) {
        // No interception of the queue supplier.
        return asIterable(batchSize, supplier).stream();
    }

    /**
     * Consumes the items from the upstream {@link Multi} as a blocking stream.
     *
     * @param contextSupplier the context supplier, must not be {@code null}, must not return {@code null}
     * @param batchSize the number of element stored in the queue
     * @param queueSupplier the supplier of queue used internally, must not be {@code null}, must not return {@code null}
     * @return a blocking stream used to consume the items from {@link Multi}
     */
    @CheckReturnValue
    public Stream<T> asStream(Supplier<Context> contextSupplier, int batchSize, Supplier<Queue<T>> queueSupplier) {
        // No interception of the queue supplier.
        return asIterable(contextSupplier, batchSize, queueSupplier).stream();
    }

}
