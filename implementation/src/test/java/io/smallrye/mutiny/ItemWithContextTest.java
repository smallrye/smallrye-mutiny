package io.smallrye.mutiny;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ItemWithContextTest {

    @Test
    void sanityCheck() {
        ItemWithContext<Integer> itemWithContext = new ItemWithContext<>(Context.of("foo", "bar"), 123);

        assertThat(itemWithContext.context().<String> get("foo")).isEqualTo("bar");
        assertThat(itemWithContext.get()).isEqualTo(123);

        ItemWithContext<Integer> copy = new ItemWithContext<>(Context.of("foo", "bar"), 123);
        ItemWithContext<Integer> diff1 = new ItemWithContext<>(Context.of("foo", "baz"), 123);
        ItemWithContext<Integer> diff2 = new ItemWithContext<>(Context.of("foo", "bar"), 456);

        assertThat(copy).isEqualTo(itemWithContext);
        assertThat(diff1).isNotEqualTo(itemWithContext);
        assertThat(diff2).isNotEqualTo(itemWithContext);

        assertThat(copy.toString()).isEqualTo("ItemWithContext{context=Context{entries={foo=bar}}, item=123}");
    }

}
