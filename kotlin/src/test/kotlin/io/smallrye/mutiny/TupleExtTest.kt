package io.smallrye.mutiny

import io.smallrye.mutiny.tuples.Tuple2
import io.smallrye.mutiny.tuples.Tuple3
import io.smallrye.mutiny.tuples.Tuple4
import io.smallrye.mutiny.tuples.Tuple5
import io.smallrye.mutiny.tuples.Tuple6
import io.smallrye.mutiny.tuples.Tuple7
import io.smallrye.mutiny.tuples.Tuple8
import io.smallrye.mutiny.tuples.Tuple9
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat

class TupleExtTest {

    @Test
    fun `test Tuple2 destructuring`() {
        val tuple = Tuple2.of("a", 1)
        val (first, second) = tuple
        assertThat(first).isEqualTo("a")
        assertThat(second).isEqualTo(1)
    }

    @Test
    fun `test Tuple3 destructuring`() {
        val tuple = Tuple3.of("a", 1, true)
        val (first, second, third) = tuple
        assertThat(first).isEqualTo("a")
        assertThat(second).isEqualTo(1)
        assertThat(third).isTrue()
    }

    @Test
    fun `test Tuple4 destructuring`() {
        val tuple = Tuple4.of("a", 1, true, 2.0)
        val (first, second, third, fourth) = tuple
        assertThat(first).isEqualTo("a")
        assertThat(second).isEqualTo(1)
        assertThat(third).isTrue()
        assertThat(fourth).isEqualTo(2.0)
    }

    @Test
    fun `test Tuple5 destructuring`() {
        val tuple = Tuple5.of("a", 1, true, 2.0, 'x')
        val (first, second, third, fourth, fifth) = tuple
        assertThat(first).isEqualTo("a")
        assertThat(second).isEqualTo(1)
        assertThat(third).isTrue()
        assertThat(fourth).isEqualTo(2.0)
        assertThat(fifth).isEqualTo('x')
    }

    @Test
    fun `test Tuple6 destructuring`() {
        val tuple = Tuple6.of("a", 1, true, 2.0, 'x', 6L)
        val (first, second, third, fourth, fifth, sixth) = tuple
        assertThat(first).isEqualTo("a")
        assertThat(second).isEqualTo(1)
        assertThat(third).isTrue()
        assertThat(fourth).isEqualTo(2.0)
        assertThat(fifth).isEqualTo('x')
        assertThat(sixth).isEqualTo(6L)
    }

    @Test
    fun `test Tuple7 destructuring`() {
        val tuple = Tuple7.of("a", 1, true, 2.0, 'x', 6L, 7.toShort())
        val (first, second, third, fourth, fifth, sixth, seventh) = tuple
        assertThat(first).isEqualTo("a")
        assertThat(second).isEqualTo(1)
        assertThat(third).isTrue()
        assertThat(fourth).isEqualTo(2.0)
        assertThat(fifth).isEqualTo('x')
        assertThat(sixth).isEqualTo(6L)
        assertThat(seventh).isEqualTo(7.toShort())
    }

    @Test
    fun `test Tuple8 destructuring`() {
        val tuple = Tuple8.of("a", 1, true, 2.0, 'x', 6L, 7.toShort(), 8.toByte())
        val (first, second, third, fourth, fifth, sixth, seventh, eighth) = tuple
        assertThat(first).isEqualTo("a")
        assertThat(second).isEqualTo(1)
        assertThat(third).isTrue()
        assertThat(fourth).isEqualTo(2.0)
        assertThat(fifth).isEqualTo('x')
        assertThat(sixth).isEqualTo(6L)
        assertThat(seventh).isEqualTo(7.toShort())
        assertThat(eighth).isEqualTo(8.toByte())
    }

    @Test
    fun `test Tuple9 destructuring`() {
        val tuple = Tuple9.of("a", 1, true, 2.0, 'x', 6L, 7.toShort(), 8.toByte(), 9.0f)
        val (first, second, third, fourth, fifth, sixth, seventh, eighth, ninth) = tuple
        assertThat(first).isEqualTo("a")
        assertThat(second).isEqualTo(1)
        assertThat(third).isTrue()
        assertThat(fourth).isEqualTo(2.0)
        assertThat(fifth).isEqualTo('x')
        assertThat(sixth).isEqualTo(6L)
        assertThat(seventh).isEqualTo(7.toShort())
        assertThat(eighth).isEqualTo(8.toByte())
        assertThat(ninth).isEqualTo(9.0f)
    }

    @Test
    fun `test Tuple2 destructuring with nullable values`() {
        val tuple: Tuple2<String?, String?> = Tuple2.of(null, null)
        val (first, second) = tuple
        assertThat(first).isNull()
        assertThat(second).isNull()
    }
}
