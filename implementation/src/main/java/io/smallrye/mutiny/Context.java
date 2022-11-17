package io.smallrye.mutiny;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A context allows sharing key / value entries along with a subscriber in a Mutiny pipeline, so all operators can
 * share implicit data for a given subscription.
 * <p>
 * A context is provided by a {@link io.smallrye.mutiny.subscription.UniSubscriber} or {@link Flow.Subscriber}
 * that implements {@link io.smallrye.mutiny.subscription.ContextSupport}.
 * <p>
 * Context keys are represented as {@link String} while values can be from heterogeneous types.
 * <p>
 * {@link Context} instances are thread-safe.
 * Internal storage is not allocated until the first entry is being added.
 * <p>
 * Contexts shall be primarily used to share transient data used for networked I/O processing such as correlation
 * identifiers, tokens, etc.
 * They should not be used as general-purpose data structures that are frequently updated and that hold large amounts of
 * data.
 *
 * @see Uni#withContext(BiFunction)
 * @see Uni#attachContext()
 * @see io.smallrye.mutiny.groups.UniSubscribe
 * @see Multi#withContext(BiFunction)
 * @see Multi#attachContext()
 * @see io.smallrye.mutiny.groups.MultiSubscribe
 */
public final class Context {

    /**
     * Creates a new empty context.
     *
     * @return the context
     */
    public static Context empty() {
        return new Context();
    }

    /**
     * Creates a context from key / value pairs.
     *
     * @param entries a sequence of key / value pairs, as in {@code key1, value1, key2, value2}
     * @return the new context
     * @throws IllegalArgumentException when {@code entries} is not balanced
     * @throws NullPointerException when {@code entries} is {@code null}
     */
    public static Context of(Object... entries) {
        requireNonNull(entries, "The entries array cannot be null");
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Arguments must be balanced to form (key, value) pairs");
        }
        HashMap<String, Object> map = new HashMap<>();
        for (int i = 0; i < entries.length; i = i + 2) {
            String key = nonNull(entries[i], "key").toString();
            Object value = nonNull(entries[i + 1], "value");
            map.put(key, value);
        }
        return new Context(map);
    }

    /**
     * Creates a context by copying the entries from a {@link Map}.
     *
     * @param entries the map, cannot be {@code null}
     * @return the new context
     * @throws NullPointerException when {@code entries} is null
     */
    public static Context from(Map<String, ?> entries) {
        return new Context(requireNonNull(entries, "The entries map cannot be null"));
    }

    private volatile ConcurrentHashMap<String, Object> entries;

    private Context() {
        this.entries = null;
    }

    private Context(Map<String, ?> initialEntries) {
        this.entries = new ConcurrentHashMap<>(initialEntries);
    }

    /**
     * Checks whether the context has an entry for a given key.
     *
     * @param key the key
     * @return {@code true} when there is an entry for {@code key}, {@code false} otherwise
     */
    public boolean contains(String key) {
        if (entries == null) {
            return false;
        } else {
            return entries.containsKey(key);
        }
    }

    /**
     * Get a value for a key.
     *
     * @param key the key
     * @param <T> the value type
     * @return the value
     * @throws NoSuchElementException when there is no entry for {@code key}
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) throws NoSuchElementException {
        if (entries == null) {
            throw new NoSuchElementException("The context is empty");
        }
        T value = (T) entries.get(key);
        if (value == null) {
            throw new NoSuchElementException("The context does not have a value for key " + key);
        }
        return value;
    }

    /**
     * Get a value for a key, or provide an alternative value when not present.
     *
     * @param key the key
     * @param alternativeSupplier the alternative value supplier when there is no entry for {@code key}
     * @param <T> the value type
     * @return the value
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrElse(String key, Supplier<? extends T> alternativeSupplier) {
        if (entries != null) {
            T value = (T) entries.get(key);
            if (value != null) {
                return value;
            }
        }
        return alternativeSupplier.get();
    }

    /**
     * Stores a value for a given key.
     *
     * @param key the key
     * @param value the value, cannot be {@code null}
     * @return this context
     */
    public Context put(String key, Object value) {
        if (entries == null) {
            synchronized (this) {
                if (entries == null) {
                    this.entries = new ConcurrentHashMap<>(8);
                }
            }
        }
        entries.put(key, value);
        return this;
    }

    /**
     * Delete an entry for a given key, if present.
     *
     * @param key the key
     * @return this context
     */
    public Context delete(String key) {
        if (entries != null) {
            entries.remove(key);
        }
        return this;
    }

    /**
     * Test whether the context is empty.
     *
     * @return {@code true} if the context is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return (this.entries == null) || (entries.isEmpty());
    }

    /**
     * Gives the set of keys present in the context at the time the method is being called.
     * <p>
     * Note that each call to this method produces a copy.
     *
     * @return the set of keys
     */
    public Set<String> keys() {
        if (this.entries == null) {
            return Collections.emptySet();
        }
        HashSet<String> set = new HashSet<>();
        Enumeration<String> enumeration = entries.keys();
        while (enumeration.hasMoreElements()) {
            set.add(enumeration.nextElement());
        }
        return set;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Context context = (Context) other;
        return Objects.equals(entries, context.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }

    @Override
    public String toString() {
        return "Context{" +
                "entries=" + entries +
                '}';
    }
}
