import ParseLib.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ParseExtraTest {
    class TermType (start : Int, len : Int) : Terminal (start, len) {
        override fun equals(other: Any?): Boolean =
                other is TermType && super.equals(other)
    }

    class SeqType (start : Int, len : Int, children : Array<Node>) : Sequence(start, len, children) {
        override fun equals(other: Any?): Boolean =
                other is SeqType && super.equals(other)
    }

    class Vector (start : Int, len : Int, children : Array<Node>) : Sequence(start, len, children) {
        override fun equals(other: Any?): Boolean =
                other is Vector && super.equals(other)
    }

    @Test
    fun test0 () {
        assertEquals(Terminal(0,5),regex("a+")().parse("aaaaa"))
        assertEquals(TermType(0,5),regex("a+",type=::TermType)().parse("aaaaa"))
        assertNull(regex("a+")().parse("bbaaa"))
        assertNull(regex("a+")().parse("aaabb"))
        assertEquals(Sequence(0,5,arrayOf(
                Terminal(0,2),
                Terminal(2,3)
        )),
                and(term("bb"), regex("a+"))().parse("bbaaa"))
        assertEquals(Sequence(0,5,arrayOf(
                Terminal(0,3),
                Terminal(3,2)
        )),
                and(regex("a+"),term("bb"))().parse("aaabb"))
        assertNull(and(regex("a+"),term("aa"))().parse("aaaaa"))
    }

    @Test
    fun test1 () {
        for (j in 5 until 10)
            for (i in 0 until 5)
                assertEquals(
                        Sequence(0,5,arrayOf (
                                Terminal(0,1),
                                Terminal(1,1),
                                Terminal(2,1),
                                Terminal(3,1),
                                Terminal(4,1)
                        ))
                , repeat(term("a"),i,j)().parse("aaaaa"))

        assertNull(repeat(term("a"),2,4)().parse("aaaaa"))

        assertEquals(
                Sequence(0,5,arrayOf(
                    Sequence(0,3,arrayOf (
                            Terminal(0,1),
                            Terminal(1,1),
                            Terminal(2,1)
                    )),
                    SeqType(3,2,arrayOf(
                            Terminal(3,1),
                            Terminal(4,1)
                    ))
                )),
                and(repeat(term("a"),1,3),repeat(term("b"),1,3,type=::SeqType))().parse("aaabb"))
    }

    val literal =
            or(
                    regex("\\d+\\.\\d+"),
                    regex("\\d+\\."),
                    regex("\\.\\d+"),
                    regex("\\d+"),
                    type = { s, l, _ -> FormulaGrammarExplicitTest.Literal(s, l) }
            )

    val add =
            and(::primaryExpr, term("+"), ::primaryExpr, type = { s, l, c -> FormulaGrammarExplicitTest.Add(s, l, arrayOf(c[0], c[2])) })

    val mul =
            and(::primaryExpr, term("*"), ::primaryExpr, type = { s, l, c -> FormulaGrammarExplicitTest.Mul(s, l, arrayOf(c[0], c[2])) })

    fun expr(): Parser<Node> =
            or(mul, add, ::primaryExpr)()

    fun primaryExpr() =
            or(
                    and(term("("), ::expr, term(")"), type = { s, l, c -> FormulaGrammarExplicitTest.Parentheses(s, l, c[1]) }),
                    literal
            )()

    val vector =
            and(term("["),repeat(and(::primaryExpr, term(",")), 0,100),::primaryExpr,term("]"),
                    type = {s,l,c ->
                        Vector(s,l,((c[1] as Sequence).children.map { (it as Sequence).children[0] } + c[2]).toTypedArray())
                    }
            )

    @Test
    fun test2 () {
        assertEquals(Vector(0,7,arrayOf(
                FormulaGrammarExplicitTest.Literal(1,1),
                FormulaGrammarExplicitTest.Literal(3,1),
                FormulaGrammarExplicitTest.Literal(5,1)
        )), vector ().parse ("[1,2,3]"))
    }
}