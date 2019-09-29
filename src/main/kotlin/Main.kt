fun indent(depth : Int) =
    "\t".repeat(depth)

abstract class Node (val start : Int, val len : Int) {
    abstract fun print (str : String, depth : Int = 0) : String
    fun matched (str : String) = str.substring (start, start+len)
}

open class Terminal (start : Int, len : Int) : Node (start, len) {
    override fun print(str : String, depth : Int) = indent(depth) + "Terminal(" + matched(str) + ")"
}

open class Sequence (start : Int, len : Int, private val children : Array<Node>) : Node (start, len) {
    override fun print(str: String, depth : Int): String {
        return indent(depth) + "Sequence(\n" + printChildren(str, depth) + indent(depth) + ")"
    }
    fun printChildren(str : String, depth : Int) =
        children.joinToString(",\n") { c -> c.print(str, depth + 1) } + "\n"
}

interface Parser <out T : Node> {
    fun parse (str : String, start : Int = 0, next : ParseNext? = null) : T?
}

interface ParseNext {
    fun next (pos : Int) : Boolean
}

typealias LazyParser <T> = () -> Parser<T>

fun parseAnd (startSub : Int, children : Array<Node?>, len : Array<Int>, str : String, startPos : Int, next : ParseNext?, vararg sub : LazyParser<Node>) : Boolean {
    assert (sub.size > startSub)

    sub [startSub]().parse(str, startPos, if (sub.size > startSub + 1) object : ParseNext {
        override fun next (pos : Int) : Boolean {
            return parseAnd (startSub + 1, children, len, str, pos, next, *sub)
        }
    } else next )?.let {
        children[startSub] = it
        len[0] += it.len
        return true
    }
    return false
}

fun <T : Node> and (vararg sub : LazyParser<Node>, type : (Int, Int, Array<Node>) -> T) : LazyParser<T> = {
    object : Parser<T> {
        override fun parse(str: String, start: Int, next: ParseNext?): T? {
            val children = arrayOfNulls<Node> (sub.size)
            val len = arrayOf(0)
            return if (parseAnd(0, children, len, str, start, next, *sub))
                @Suppress("UNCHECKED_CAST")
                type (start, len[0], children as Array<Node>)
            else
                null
        }
    }
}

fun and (vararg sub : LazyParser<Node>) : LazyParser<Sequence> =
    and(*sub, type = { s, l, c -> Sequence(s,l,c) })

fun <T : Node> or (vararg sub : LazyParser<Node>, type : (Int, Int, Node) -> T) : LazyParser<T> {
    assert (sub.isNotEmpty())
    return {
        object : Parser<T> {
            override fun parse(str: String, start: Int, next: ParseNext?): T? {
                for (slp in sub) {
                    val sp = slp ()
                    sp.parse(str, start, next)?.let { return type (it.start, it.len, it); }
                }
                return null
            }
        }
    }
}

fun or (vararg sub : LazyParser<Node>) : LazyParser<Node> =
    or(*sub, type = { _, _, n -> n })


fun <T : Terminal> term (expect : String, type : (Int, Int) -> T) : LazyParser<T> = {
    object : Parser<T> {
        override fun parse(str: String, start: Int, next: ParseNext?): T? {
            return if (
                ((start+expect.length < str.length && next != null)
                ||  (start+expect.length == str.length))
                &&  str.substring(start, start+expect.length) == expect
                &&  (next == null || next.next(start+expect.length))
                    ) {
                type (start, expect.length)
            } else null
        }
    }
}

fun term (expect : String) : LazyParser<Terminal> =
    term(expect, type = {s, l -> Terminal (s, l) })

class Literal (start : Int, len : Int): Node (start, len) {
    override fun print(str : String, depth : Int) = indent(depth) + "Literal(" + matched(str) + ")"
}

class Add (start : Int, len : Int, children : Array<Node>) : Sequence(start, len, children) {
    override fun print(str : String, depth : Int) = indent(depth) + "Add(\n" + printChildren(str, depth) + indent(depth) + ")"
}

class Mul (start : Int, len : Int, children : Array<Node>) : Sequence(start, len, children) {
    override fun print(str : String, depth : Int) = indent(depth) + "Mul(\n" + printChildren(str, depth) + indent(depth) + ")"
}

class Parentheses (start : Int, len : Int, child : Node) : Sequence(start, len, arrayOf(child)) {
    override fun print(str : String, depth : Int) = indent(depth) + "Parentheses(\n" + printChildren(str, depth) + indent(depth) + ")"
}

val digit =
    or (term("0"), term("1"), term("2"), term ("3"), term ("4"), term ("5"), term ("6"), term ("7"), term ("8"), term ("9"))

fun digits () : Parser<Node> =
    or(and(digit, ::digits), digit)()

val literal =
    or(
        and(::digits,term("."),::digits),
        and(term("."),::digits),
        and(::digits,term(".")),
        ::digits,
        type = { s,l,_ -> Literal (s,l) }
    )

val add =
    and(::primaryExpr, term("+"), ::primaryExpr, type = {s,l,c -> Add(s,l,arrayOf(c[0], c[2]))})

val mul =
    and(::primaryExpr, term("*"), ::primaryExpr, type = {s,l,c -> Mul(s,l,arrayOf(c[0], c[2]))})

fun expr () : Parser<Node> =
    or(mul, add, ::primaryExpr)()

fun primaryExpr () =
    or(
        and(term("("),::expr,term(")"), type = {s,l,c -> Parentheses(s,l,c[1]) }),
        literal
    )()

fun main (args : Array<String>) {
    val str = if(args.isEmpty()) "(2.123*(4+5))+(2*(7+(4*3)))" else args[0]

    val parser = expr()

    val ast = parser.parse(str, 0)
    if(ast == null)
        println("Parse error")
    else
        print(ast.print(str))
}