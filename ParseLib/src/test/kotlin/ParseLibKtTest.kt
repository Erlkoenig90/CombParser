import org.junit.jupiter.api.Test
import ParseLib.*

import org.junit.jupiter.api.Assertions.*

internal class ParseLibKtTest {
    class SeqType (start : Int, len : Int, children : Array<Node>) : Sequence(start, len, children) {
        override fun equals(other: Any?): Boolean =
            other is SeqType && super.equals(other)
    }

    @Test
    fun term() {
        assertEquals(Terminal(0, 1),term("1")().parse("1"))
        assertEquals(Terminal(0, 3),term("123") ().parse("123"))
        assertNull(term("124") ().parse("123"))
        assertNull(term("123") ().parse("12"))
        assertNull(term("123") ().parse("23"))
    }

    @Test
    fun and() {
        assertEquals(Sequence(0, 2, arrayOf(Terminal(0, 1), Terminal(1, 1))),and(term("1"), term("2"))().parse("12"))
        assertEquals(SeqType(0, 2, arrayOf(Terminal(0, 1), Terminal(1, 1))),and(term("1"), term("2"), type=::SeqType)().parse("12"))
        assertEquals(Sequence(0, 3, arrayOf(Terminal(0, 1),
                        Sequence(1,2,arrayOf(
                                    Terminal(1, 1),
                                    Terminal(2, 1))))),and(term("1"), and(term("2"), term("3")))().parse("123"))

        assertNull(and(term("1"), term("2"))().parse("1"))
        assertNull(and(term("1"), term("2"))().parse("2"))
    }

    class TermType (start : Int, len : Int) : Terminal (start, len) {
        override fun equals(other: Any?): Boolean =
                other is TermType && super.equals(other)
    }

    @Test
    fun or() {
        assertEquals(Terminal(0,1),or(term("1"), term("2", type=::TermType))().parse("1"))
        assertEquals(TermType(0,1),or(term("1"), term("2", type=::TermType))().parse("2"))

        assertEquals(SeqType(0,2,arrayOf(Terminal(0,1),Terminal(1,1))), or(and(term("1"),term("2")), and(term("1"), term("3"),type=::SeqType))().parse("13"))
    }


}