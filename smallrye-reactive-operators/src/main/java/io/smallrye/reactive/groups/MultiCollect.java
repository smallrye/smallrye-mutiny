package io.smallrye.reactive.groups;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.MultiCollector;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class MultiCollect<T> {

    private final Multi<T> upstream;

    public MultiCollect(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Creates a {@link Uni} receiving the <strong>first</strong> result emitted by the upstream {@link Multi}.
     * If the {@link Multi} is empty, the produced {@link Uni} fires {@code null} as result when the {@link Multi}
     * emits the completion event. If the {@link Multi} emits a failure before having emitted a result, the produced
     * {@link Uni} propagates the failure.
     *
     * @return the produced uni
     */
    public Uni<T> first() {
        return MultiCollector.first(upstream);
    }

    /**
     * Creates a {@link Uni} receiving the <strong>last</strong> result emitted by the upstream {@link Multi}.
     * The last result is result fired just before the completion event.
     * <p>
     * If the {@link Multi} is empty, the produced {@link Uni} fires {@code null} as result when the {@link Multi}
     * emits the completion event. If the {@link Multi} emits a failure, the produced {@link Uni} propagates the
     * failure.
     *
     * @return the produced uni
     */
    public Uni<T> last() {
        return MultiCollector.last(upstream);
    }


    /**
     * Creates a {@link Uni} emitting a result containing all elements emitted by this {@link Multi} into a
     * {@link List}. The produced {@link Uni} emits its result when this {@link Multi} completes.
     *
     * @return the {@link Uni} emitting the list of results from this {@link Multi}.
     */
    public Uni<List<T>> asList() {
        return MultiCollector.list(upstream);
    }


    /**
     * Creates a {@link Uni} emitting a result with the object computed by the given {@link Collector}.
     * The collector behaves the same way as on a Java stream.
     *
     * @param collector the {@link Collector}, must not be {@code null}
     * @param <A>       the accumulation type
     * @param <X>       the result type
     * @return a {@link Uni} emitted the collected object as result, when the {@link Multi} completes
     */
    public <X, A> Uni<X> with(Collector<? super T, A, ? extends X> collector) {
        return MultiCollector.collector(upstream, collector);
    }

    /**
     * Produces a new {@link Uni} emitting a <em>container</em> with all results emitted by this {@link Multi}.
     * <p>
     * It produces the container instance using the passed {@link Supplier} (at subscription time) and then call the
     * {@code accumulator} bi-consumer for each result emitted by the {@link Multi}.
     * <p>
     * The collected result will be emitted when this {@link Multi} fires the completion event.
     * <p>
     * If the {@link Multi} propagates a failure, the produces {@link Uni} propagates the same failure, even if some
     * results have been collected.
     * If the {@link Multi} is empty, the supplied container is returned <em>empty</em>
     *
     * @param supplier    the supplier of the container instance, called at Subscription time. Must not be {@code null}.
     *                    Must not produce {@code null}
     * @param accumulator a consumer called on every result with the container instance and the result. It should
     *                    <em>add</em> the result into the container. Must not be {@code null}
     * @param <X>         the type of the container produced by the supplier.
     * @return a {@link Uni} emitting the collected container as result when this {@link Multi} completes
     */
    public <X> Uni<X> in(Supplier<X> supplier, BiConsumer<X, T> accumulator) {
        return MultiCollector.collectInto(upstream, supplier, accumulator);
    }

    /**
     * Produces an {@link Uni} emitting a {@link Map} of <code>key -&gt; result</code>  for each result emitted by this
     * {@link Multi}. The collected map is emitted by the produced {@link Uni} when the {@link Multi} fires the
     * completion event.
     * <p>
     * The key is extracted from each result by applying the {@code keyMapper} function. In case of conflict,
     * the associated value will be the most recently emitted result.
     *
     * @param keyMapper a {@link Function} to map result to a key for the {@link Map}. Must not be {@code null},
     *                  must not produce {@code null}
     * @param <K>       the type of the key extracted from each result emitted by this {@link Multi}
     * @return a {@link Uni} emitting a result with the collected {@link Map}. The uni emits the result when this
     * {@link Multi} completes
     */
    public <K> Uni<Map<K, T>> asMap(Function<? super T, ? extends K> keyMapper) {
        return asMap(keyMapper, Function.identity());
    }

    /**
     * Produces an {@link Uni} emitting a {@link Map} of <code>key -&gt; mapped result</code>  for each result emitted by
     * this {@link Multi}. The collected map is emitted by the produced {@link Uni} when the {@link Multi} fires the
     * completion event.
     * <p>
     * The key is extracted from each result by applying the {@code keyMapper} function. In case of conflict,
     * the associated value will be the most recently emitted result. The value is computed by applying the
     * {@code valueMapper} function.
     *
     * @param keyMapper   a {@link Function} to map result to a key for the {@link Map}. Must not be {@code null},
     *                    must not produce {@code null}
     * @param valueMapper a {@link Function} to map result to a value for the {@link Map}. Must not be {@code null},
     *                    must not produce {@code null}
     * @param <K>         the type of the key extracted from each result emitted by this {@link Multi}
     * @param <V>         the type of the value extracted from each result emitted by this {@link Multi}
     * @return a {@link Uni} emitting a result with the collected {@link Map}. The uni emits the result when this
     * {@link Multi} completes
     */
    public <K, V> Uni<Map<K, V>> asMap(Function<? super T, ? extends K> keyMapper,
                                       Function<? super T, ? extends V> valueMapper) {
        return MultiCollector.map(upstream, keyMapper, valueMapper);
    }

    /**
     * Produces an {@link Uni} emitting a {@link Map} of <code>key -&gt; Collection of mapped results</code>  for each
     * result emitted by this {@link Multi}. The collected map is emitted by the produced {@link Uni} when the
     * {@link Multi} fires the completion event.
     * <p>
     * The key is extracted from each result by applying the {@code keyMapper} function.
     * The value is a collection containing all the values mapped to the specific key. The value is computed
     * by applying the {@code valueMapper} function.
     *
     * @param keyMapper   a {@link Function} to map result to a key for the {@link Map}. Must not be {@code null},
     *                    must not produce {@code null}
     * @param valueMapper a {@link Function} to map result to a value for the {@link Map}. Must not be {@code null},
     *                    must not produce {@code null}
     * @param <K>         the type of the key extracted from each result emitted by this {@link Multi}
     * @param <V>         the type of the value extracted from each result emitted by this {@link Multi}
     * @return a {@link Uni} emitting a result with the collected {@link Map}. The uni emits the result when this
     * {@link Multi} completes
     */
    public <K, V> Uni<Map<K, Collection<V>>> asMultiMap(Function<? super T, ? extends K> keyMapper,
                                                        Function<? super T, ? extends V> valueMapper) {
        return MultiCollector.multimap(upstream, keyMapper, valueMapper);
    }

    /**
     * Produces an {@link Uni} emitting a {@link Map} of <code>key -&gt; Collection of results</code>  for each
     * result emitted by this {@link Multi}. The collected map is emitted by the produced {@link Uni} when the
     * {@link Multi} fires the completion event.
     * <p>
     * The key is extracted from each result by applying the {@code keyMapper} function.
     * The value is a collection containing all the results the to the specific key.
     *
     * @param keyMapper a {@link Function} to map result to a key for the {@link Map}.Must not be {@code null},
     *                  must not produce {@code null}
     * @param <K>       the type of the key extracted from each result emitted by this {@link Multi}
     * @return a {@link Uni} emitting a result with the collected {@link Map}. The uni emits the result when this
     * {@link Multi} completes
     */
    public <K> Uni<Map<K, Collection<T>>> asMultiMap(Function<? super T, ? extends K> keyMapper) {
        return MultiCollector.multimap(upstream, keyMapper, Function.identity());
    }
}
