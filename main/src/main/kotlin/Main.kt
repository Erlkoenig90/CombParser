import ParseLib.*

class Literal (start : Int, len : Int): Node(start, len) {
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

    val ast = parser.parse(str)
    if(ast == null)
        println("Parse error")
    else
        print(ast.print(str))
}