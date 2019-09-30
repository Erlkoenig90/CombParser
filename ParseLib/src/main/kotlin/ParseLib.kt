// Basic required parser core functions

package ParseLib

abstract class Parser <out T : Node> {
    internal abstract fun parseInternal (ctx : ParseContext, start : Int = 0, next : ParseNext<T>) : ParseContState

    fun parse (str : String) : T? {
        var res : T? = null
        val ctx = ParseContext(str)
        val receiveNext = object : ParseNext<Node> {
            override fun next(pos: Int, node: Node): ParseContState {
                return if(pos == str.length) {
                    @Suppress("UNCHECKED_CAST")
                    res = node as T
                    ParseContState.OK
                } else
                    ParseContState.FAIL
            }
        }

        var state = parseInternal(ctx, 0, receiveNext)
        while (state != ParseContState.OK) {
            if(ctx.branchQueue.isEmpty()) return null
            val next = ctx.branchQueue.first()
            ctx.branchQueue.removeAt(0)
            state = next(ctx)
        }
        return res
    }
}

internal interface ParseNext <in T : Node> {
    fun next (pos : Int, node : T) : ParseContState
}

internal typealias Continuation = (ctx : ParseContext) -> ParseContState;

internal data class ParseContext (val str : String, val branchQueue: MutableList<Continuation> = mutableListOf())

typealias LazyParser <T> = () -> Parser<T>

internal fun <T : Node> parseAnd (startSub : Int, children : Chain<Node>?, len : Int, ctx: ParseContext, currentPos : Int, startPos : Int, next : ParseNext<T>, type : (Int, Int, Array<Node>) -> T, vararg sub : LazyParser<Node>) : ParseContState {
    assert (sub.size > startSub)

    return sub [startSub]().parseInternal(ctx, currentPos,
            if (sub.size > startSub + 1)
                object : ParseNext<Node> {
                    override fun next (pos : Int, node : Node) : ParseContState {
                        val newChildren = addChain (children, node)

                        return parseAnd (startSub + 1, newChildren, len + node.len, ctx, pos, startPos, next, type, *sub)
                    }
                }
            else
                object : ParseNext<Node> {
                    override fun next (pos : Int, node : Node) : ParseContState {
                        val newChildren = addChain (children, node)

                        return next.next(pos, type(startPos, len + node.len, toReverseArray(newChildren)))
                    }
                }
    )
}

fun <T : Node> and (vararg sub : LazyParser<Node>, type : (Int, Int, Array<Node>) -> T) : LazyParser<T> = {
    object : Parser<T> () {
        override fun parseInternal(ctx: ParseContext, start: Int, next: ParseNext<T>) : ParseContState {
            return parseAnd (0, null, 0, ctx, start, start, next, type, *sub)
        }
    }
}

fun and (vararg sub : LazyParser<Node>) : LazyParser<Sequence> =
        and(*sub, type = { s, l, c -> Sequence(s,l,c) })

fun <T : Node> or (vararg sub : LazyParser<Node>, type : (Int, Int, Node) -> T) : LazyParser<T> {
    assert (sub.isNotEmpty())
    return {
        object : Parser<T> () {
            override fun parseInternal(ctx: ParseContext, start: Int, next: ParseNext<T>) : ParseContState {
                ctx.branchQueue.add(fun (ctxi : ParseContext) : ParseContState {
                    var res = ParseContState.FAIL
                    for (slp in sub) {
                        val sp = slp ()
                        when (sp.parseInternal(ctxi, start, object : ParseNext<Node> {
                            override fun next (pos : Int, node : Node) =
                                    next.next (pos, type (start, pos-start, node))
                        })) {
                            ParseContState.OK -> return ParseContState.OK
                            ParseContState.SUSPEND -> res = ParseContState.SUSPEND
                            ParseContState.FAIL -> {}
                        }
                    }
                    return res
                })

                return ParseContState.SUSPEND
            }
        }
    }
}

fun or (vararg sub : LazyParser<Node>) : LazyParser<Node> =
        or(*sub, type = { _, _, n -> n })


fun <T : Terminal> term (expect : String, type : (Int, Int) -> T) : LazyParser<T> = {
    object : Parser<T> () {
        override fun parseInternal(ctx: ParseContext, start: Int, next: ParseNext<T>) : ParseContState {
            return if((start+expect.length <= ctx.str.length)
                    &&  ctx.str.substring(start, start+expect.length) == expect)
                next.next(start+expect.length, type (start, expect.length))
            else
                ParseContState.FAIL
        }
    }
}

fun term (expect : String) : LazyParser<Terminal> =
        term(expect, type = ::Terminal)

