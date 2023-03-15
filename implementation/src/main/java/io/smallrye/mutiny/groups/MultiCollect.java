package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiCollectorOp;
import io.smallrye.mutiny.operators.multi.MultiLastItemOp;

/**
 * Collects / aggregates items from the upstream and send the resulting <em>collection</em> / structure when the
 * upstream completes. The resulting structure is emitted through a {@link Uni}.
 * <p>
 * <strong>IMPORTANT:</strong> Do not use on unbounded streams, as it would lead to {@link OutOfMemoryError}.
 *
 * @param <T> the type of item sent by the upstream.
 */
public class MultiCollect<T> {

    private final Multi<T> upstream;

    public MultiCollect(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Creates a {@link Uni} receiving the <strong>first</strong> item emitted by the upstream {@link Multi}.
     * If the {@link Multi} is empty, the produced {@link Uni} fires {@code null} as item when the {@link Multi}
     * emits the completion event. If the {@link Multi} emits a failure before having emitted an item, the produced
     * {@link Uni} propagates the failure.
     *
     * @return the produced uni
     */
    @CheckReturnValue
    public Uni<T> first() {
        return Uni.createFrom().multi(upstream);
    }

    /**
     * Creates a {@link Uni} receiving the <strong>last</strong> item emitted by the upstream {@link Multi}.
     * The last item is item fired just before the completion event.
     * <p>
     * If the {@link Multi} is empty, the produced {@link Uni} fires {@code null} as item when the {@link Multi}
     * emits the completion event. If the {@link Multi} emits a failure, the produced {@link Uni} propagates the
     * failure.
     *
     * @return the produced uni
     */
    @CheckReturnValue
    public Uni<T> last() {
        return Uni.createFrom().publisher(Infrastructure.onMultiCreation(new MultiLastItemOp<>(upstream)));
    }

    /**
     * Creates a {@link Uni} emitting an item containing all elements emitted by this {@link Multi} into a
     * {@link List}. The produced {@link Uni} emits its item when this {@link Multi} completes.
     *
     * @return the {@link Uni} emitting the list of items from this {@link Multi}.
     */
    @CheckReturnValue
    public Uni<List<T>> asList() {
        return collector(upstream, Collectors.toList(), false);
    }

    /**
     * Creates a {@link Uni} emitting an item containing all elements emitted by this {@link Multi} into a
     * {@link Set}. The produced {@link Uni} emits its item when this {@link Multi} completes.
     *
     * @return the {@link Uni} emitting the set of items from this {@link Multi}.
     */
    @CheckReturnValue
    public Uni<Set<T>> asSet() {
        return collector(upstream, Collectors.toSet(), false);
    }

    /**
     * Creates a {@link Uni} emitting an item with the object computed by the given {@link Collector}.
     * The collector behaves the same way as on a Java stream.
     *
     * @param collector the {@link Collector}, must not be {@code null}
     * @param <A> the accumulation type
     * @param <X> the item type
     * @return a {@link Uni} emitted the collected object as item, when the {@link Multi} completes
     */
    @CheckReturnValue
    public <X, A> Uni<X> with(Collector<? super T, A, ? extends X> collector) {
        return collector(upstream, collector, true);
    }

    /**
     * Produces a new {@link Uni} emitting a <em>container</em> with all items emitted by this {@link Multi}.
     * <p>
     * It produces the container instance using the passed {@link Supplier} (at subscription time) and then call the
     * {@code accumulator} bi-consumer for each item emitted by the {@link Multi}.
     * <p>
     * The collected item will be emitted when this {@link Multi} fires the completion event.
     * <p>
     * If the {@link Multi} propagates a failure, the produces {@link Uni} propagates the same failure, even if some
     * items have been collected.
     * If the {@link Multi} is empty, the supplied container is returned <em>empty</em>
     *
     * @param supplier the supplier of the container instance, called at Subscription time. Must not be {@code null}.
     *        Must not produce {@code null}
     * @param accumulator a consumer called on every item with the container instance and the item. It should
     *        <em>add</em> the item into the container. Must not be {@code null}
     * @param <X> the type of the container produced by the supplier.
     * @return a {@link Uni} emitting the collected container as item when this {@link Multi} completes
     */
    @CheckReturnValue
    public <X> Uni<X> in(Supplier<X> supplier, BiConsumer<X, T> accumulator) {
        Supplier<X> actualSupplier = Infrastructure.decorate(nonNull(supplier, "supplier"));
        BiConsumer<X, T> actualAccumulator = Infrastructure.decorate(nonNull(accumulator, "accumulator"));
        Collector<? super T, X, X> collector = Collector.of(actualSupplier, actualAccumulator, (r, r2) -> r,
                Collector.Characteristics.IDENTITY_FINISH);
        return collector(upstream, collector, false);
    }

    /**
     * Produces an {@link Uni} emitting a {@link Map} of <code>key -&gt; item</code> for each item emitted by this
     * {@link Multi}. The collected map is emitted by the produced {@link Uni} when the {@link Multi} fires the
     * completion event.
     * <p>
     * The key is extracted from each item by applying the {@code keyMapper} function. In case of conflict,
     * the associated value will be the most recently emitted item.
     *
     * @param keyMapper a {@link Function} to map item to a key for the {@link Map}. Must not be {@code null},
     *        must not produce {@code null}
     * @param <K> the type of the key extracted from each item emitted by this {@link Multi}
     * @return a {@link Uni} emitting an item with the collected {@link Map}. The uni emits the item when this
     *         {@link Multi} completes
     */
    @CheckReturnValue
    public <K> Uni<Map<K, T>> asMap(Function<? super T, ? extends K> keyMapper) {
        Function<? super T, ? extends K> actualKM = Infrastructure.decorate(nonNull(keyMapper, "keyMapper"));
        return collector(upstream, Collectors.toMap(actualKM, Function.identity()), false);
    }

    /**
     * Produces an {@link Uni} emitting a {@link Map} of <code>key -&gt; mapped item</code> for each item emitted by
     * this {@link Multi}. The collected map is emitted by the produced {@link Uni} when the {@link Multi} fires the
     * completion event.
     * <p>
     * The key is extracted from each item by applying the {@code keyMapper} function. In case of conflict,
     * the associated value will be the most recently emitted item. The value is computed by applying the
     * {@code valueMapper} function.
     *
     * @param keyMapper a {@link Function} to map item to a key for the {@link Map}. Must not be {@code null},
     *        must not produce {@code null}
     * @param valueMapper a {@link Function} to map item to a value for the {@link Map}. Must not be {@code null},
     *        must not produce {@code null}
     * @param <K> the type of the key extracted from each item emitted by this {@link Multi}
     * @param <V> the type of the value extracted from each item emitted by this {@link Multi}
     * @return a {@link Uni} emitting an item with the collected {@link Map}. The uni emits the item when this
     *         {@link Multi} completes
     */
    @CheckReturnValue
    public <K, V> Uni<Map<K, V>> asMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        Function<? super T, ? extends K> actualKM = Infrastructure.decorate(nonNull(keyMapper, "keyMapper"));
        Function<? super T, ? extends V> actualVM = Infrastructure.decorate(nonNull(valueMapper, "valueMapper"));
        return collector(upstream, Collectors.toMap(actualKM, actualVM), false);
    }

    /**
     * Produces an {@link Uni} emitting a {@link Map} of <code>key -&gt; mapped item</code> for each item emitted by
     * this {@link Multi}. The collected map is emitted by the produced {@link Uni} when the {@link Multi} fires the
     * completion event.
     * <p>
     * The key is extracted from each item by applying the {@code keyMapper} function.
     * In case of conflict {@code mergeFunction} is used to choose which item should be emitted.
     * The value is computed by applying the {@code valueMapper} function.
     *
     * @param keyMapper a {@link Function} to map item to a key for the {@link Map}. Must not be {@code null},
     *        must not produce {@code null}
     * @param valueMapper a {@link Function} to map item to a value for the {@link Map}. Must not be {@code null},
     *        must not produce {@code null}
     * @param mergeFunction a {@link BinaryOperator} used to resolve collisions between values associated
     *        with the same key. Must not be {@code null}.
     *        In case it returns null the owner key will be removed from the {@link Map}
     * @param <K> the type of the key extracted from each item emitted by this {@link Multi}
     * @param <V> the type of the value extracted from each item emitted by this {@link Multi}
     * @return a {@link Uni} emitting an item with the collected {@link Map}. The uni emits the item when this
     *         {@link Multi} completes
     */
    @CheckReturnValue
    public <K, V> Uni<Map<K, V>> asMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction) {
        Function<? super T, ? extends K> actualKM = Infrastructure.decorate(nonNull(keyMapper, "keyMapper"));
        Function<? super T, ? extends V> actualVM = Infrastructure.decorate(nonNull(valueMapper, "valueMapper"));
        BinaryOperator<V> actualMF = Infrastructure.decorate(nonNull(mergeFunction, "mergeFunction"));
        return collector(upstream, Collectors.toMap(actualKM, actualVM, actualMF), false);
    }

    /**
     * Produces an {@link Uni} emitting a {@link Map} of <code>key -&gt; Collection of mapped values</code> for each
     * item emitted by this {@link Multi}. The collected map is emitted by the produced {@link Uni} when the
     * {@link Multi} fires the completion event.
     * <p>
     * The key is extracted from each item by applying the {@code keyMapper} function.
     * The value is a collection containing all the values mapped to the specific key. The value is computed
     * by applying the {@code valueMapper} function.
     *
     * @param keyMapper a {@link Function} to map item to a key for the {@link Map}. Must not be {@code null},
     *        must not produce {@code null}
     * @param valueMapper a {@link Function} to map item to a value for the {@link Map}. Must not be {@code null},
     *        must not produce {@code null}
     * @param <K> the type of the key extracted from each item emitted by this {@link Multi}
     * @param <V> the type of the value extracted from each item emitted by this {@link Multi}
     * @return a {@link Uni} emitting an item with the collected {@link Map}. The uni emits the item when this
     *         {@link Multi} completes
     */
    @CheckReturnValue
    public <K, V> Uni<Map<K, Collection<V>>> asMultiMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        Function<? super T, ? extends K> actualKM = Infrastructure.decorate(nonNull(keyMapper, "keyMapper"));
        Function<? super T, ? extends V> actualVM = Infrastructure.decorate(nonNull(valueMapper, "valueMapper"));
        return collector(upstream, Collectors.toMap(
                actualKM,
                res -> {
                    List<V> list = new ArrayList<>();
                    V mapped = actualVM.apply(res);
                    list.add(mapped);
                    return list;
                },
                (vs, vs2) -> {
                    vs.addAll(vs2);
                    return vs;
                }), false);
    }

    /**
     * Produces an {@link Uni} emitting a {@link Map} of <code>key -&gt; Collection of items</code> for each
     * item emitted by this {@link Multi}. The collected map is emitted by the produced {@link Uni} when the
     * {@link Multi} fires the completion event.
     * <p>
     * The key is extracted from each item by applying the {@code keyMapper} function.
     * The value is a collection containing all the items emitted associated to the specific key.
     *
     * @param keyMapper a {@link Function} to map item to a key for the {@link Map}.Must not be {@code null},
     *        must not produce {@code null}
     * @param <K> the type of the key extracted from each item emitted by this {@link Multi}
     * @return a {@link Uni} emitting an item with the collected {@link Map}. The uni emits the item when this
     *         {@link Multi} completes
     */
    @CheckReturnValue
    public <K> Uni<Map<K, Collection<T>>> asMultiMap(Function<? super T, ? extends K> keyMapper) {
        Function<? super T, ? extends K> actualKM = Infrastructure.decorate(nonNull(keyMapper, "keyMapper"));
        return collector(upstream, Collectors.toMap(
                actualKM,
                res -> {
                    List<T> list = new ArrayList<>();
                    list.add(res);
                    return list;
                },
                (vs, vs2) -> {
                    vs.addAll(vs2);
                    return vs;
                }), false);
    }

    /**
     * Collects only the items from the upstream that passes the given predicate.
     * This method is equivalent to {@code upstream.select().when(predicate).collect()}.
     * <p>
     * For each item, it calls the predicate. If the predicate returns {@code true}, it collects the item, otherwise
     * it discards the item. If the predicate throws an exception, it propagates that exception as failure.
     *
     * @param predicate the predicate, must not be {@code null}.
     * @return the object to configure the item collection.
     */
    @CheckReturnValue
    public MultiCollect<T> where(Predicate<T> predicate) {
        // Decoration happens in where.
        return new MultiCollect<>(upstream.select().where(predicate));
    }

    /**
     * Collects only the items from the upstream that passes the given predicate.
     * Unlike {@link #where(Predicate)}, the predicate returns a {@link Uni Uni&lt;Boolean&gt;}, which support asynchronous
     * tests.
     * <p>
     * This method is equivalent to {@code upstream.select().where(predicate).collect()}.
     * <p>
     * For each item, it calls the predicate. If the predicate emits the item {@code true}, it collects the item, otherwise
     * it discards the item. If the predicate throws an exception or emits a failure, it propagates that exception as
     * failure.
     *
     * @param predicate the predicate, must not be {@code null}.
     * @return the object to configure the item collection.
     */
    @CheckReturnValue
    public MultiCollect<T> when(Function<? super T, Uni<Boolean>> predicate) {
        // Decoration happens in `when`
        return new MultiCollect<>(upstream.select().when(predicate));
    }

    private static <T, A, R> Uni<R> collector(Multi<T> upstream, Collector<? super T, A, ? extends R> collector,
            boolean acceptNullAsInitialValue) {
        Multi<R> multi = Infrastructure
                .onMultiCreation(new MultiCollectorOp<>(upstream, collector, acceptNullAsInitialValue));
        return Uni.createFrom().publisher(multi);
    }

}
