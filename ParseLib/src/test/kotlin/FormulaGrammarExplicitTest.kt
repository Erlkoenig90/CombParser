import ParseLib.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class FormulaGrammarExplicitTest {

    class Literal(start: Int, len: Int) : Node(start, len) {
        override fun print(str: String, depth: Int) = indent(depth) + "Literal(" + matched(str) + ")"

        override fun equals(other: Any?): Boolean =
                other is Literal && super.equals(other)
    }

    class Add(start: Int, len: Int, children: Array<Node>) : Sequence(start, len, children) {
        override fun print(str: String, depth: Int) = indent(depth) + "Add(\n" + printChildren(str, depth) + indent(depth) + ")"

        override fun equals(other: Any?): Boolean =
                other is Add && super.equals(other)
    }

    class Mul(start: Int, len: Int, children: Array<Node>) : Sequence(start, len, children) {
        override fun print(str: String, depth: Int) = indent(depth) + "Mul(\n" + printChildren(str, depth) + indent(depth) + ")"

        override fun equals(other: Any?): Boolean =
                other is Mul && super.equals(other)
    }

    class Parentheses(start: Int, len: Int, child: Node) : Sequence(start, len, arrayOf(child)) {
        override fun print(str: String, depth: Int) = indent(depth) + "Parentheses(\n" + printChildren(str, depth) + indent(depth) + ")"

        override fun equals(other: Any?): Boolean =
                other is Parentheses && super.equals(other)
    }

    val digit =
            or(term("0"), term("1"), term("2"), term("3"), term("4"), term("5"), term("6"), term("7"), term("8"), term("9"))

    fun digits(): Parser<Node> =
            or(and(digit, ::digits), digit)()

    val literal =
            or(
                    and(::digits, term("."), ::digits),
                    and(term("."), ::digits),
                    and(::digits, term(".")),
                    ::digits,
                    type = { s, l, _ -> Literal(s, l) }
            )

    val add =
            and(::primaryExpr, term("+"), ::primaryExpr, type = { s, l, c -> Add(s, l, arrayOf(c[0], c[2])) })

    val mul =
            and(::primaryExpr, term("*"), ::primaryExpr, type = { s, l, c -> Mul(s, l, arrayOf(c[0], c[2])) })

    fun expr(): Parser<Node> =
            or(mul, add, ::primaryExpr)()

    val add2 =
            and(::expr, term("+"), ::expr, type = { s, l, c -> Add(s, l, arrayOf(c[0], c[2])) })

    val mul2 =
            and(::expr, term("*"), ::expr, type = { s, l, c -> Mul(s, l, arrayOf(c[0], c[2])) })

    fun expr2(): Parser<Node> =
            or(mul2, add2, ::primaryExpr)()

    fun primaryExpr() =
            or(
                    and(term("("), ::expr, term(")"), type = { s, l, c -> Parentheses(s, l, c[1]) }),
                    literal
            )()

    @Test
    fun test0 () {
        assertEquals(Literal(0,3),expr().parse("1.2"))
        assertEquals(Literal(0,7),expr().parse("123.456"))
        assertEquals(Literal(0,7),expr().parse(".123456"))
        assertEquals(Literal(0,7),expr().parse("123456."))
        assertNull(expr().parse("123.456."))
        assertNull(expr().parse("."))
    }

    @Test
    fun test1 () {
        assertEquals(
                Add(0, 27, arrayOf (
                    Parentheses(0, 13,
                        Mul (1, 11, arrayOf (
                            Literal(1, 5),
                            Parentheses (7, 5,
                                Add (8,3,arrayOf(
                                    Literal(8,1),
                                    Literal(10,1)
                                ))
                            )
                        ))
                    ),
                    Parentheses(14, 13,
                        Mul(15,11,arrayOf(
                            Literal(15,1),
                            Parentheses(17, 9,
                                Add(18,7, arrayOf(
                                    Literal(18,1),
                                    Parentheses(20,5,
                                        Mul(21,3,arrayOf(
                                            Literal(21,1),
                                            Literal(23,1)
                                    ))
                                    )
                                ))
                            )
                        ))
                    )
                )),
            expr().parse("(2.123*(4+5))+(2*(7+(4*3)))"))
    }

    @Test
    fun test2 () {
        assertEquals(
            Add(0,5,arrayOf(
                Add(0,3,arrayOf(
                    Literal(0,1),
                    Literal(2,1)
                )),
                Literal(4,1)
            )),
            expr2().parse("1+2+3"))
    }
}