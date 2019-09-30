// Additional optional parsing functionality

package ParseLib

import java.util.regex.Pattern

fun <T : Terminal> regex (expect : Pattern, type : (Int, Int) -> T) : LazyParser<T> = {
    object : Parser<T> () {
        override fun parseInternal(ctx: ParseContext, start: Int, next: ParseNext<T>) : ParseContState {
            val m = expect.matcher(ctx.str)
            m.region(start, ctx.str.length)


            return if(m.lookingAt())
                next.next(m.end(), type (start, m.end()-start))
            else
                ParseContState.FAIL
        }
    }
}

fun <T : Terminal> regex (expect : String, type : (Int, Int) -> T) : LazyParser<T> =
        regex(Pattern.compile(expect), type)

fun regex (expect : Pattern) : LazyParser<Terminal> =
        regex(expect, type = ::Terminal)

fun regex (expect : String) : LazyParser<Terminal> =
        regex(Pattern.compile(expect), type = ::Terminal)

internal fun <T : Node, R : Node> parseRepeatLoop (sub : Parser<T>, children : Chain<T>?, count : Int, len : Int, ctx : ParseContext, currentPos : Int, startPos : Int, next : ParseNext<R>, nodeClass: Class<T>, type : (Int, Int, Array<T>) -> R) : ParseContState {
    assert (count > 0);

    return sub.parseInternal(ctx, currentPos, if(count == 1)
        object : ParseNext<T> {
            override fun next(pos: Int, node: T): ParseContState {
                return next.next(pos, type (startPos, len+node.len, toReverseArray(addChain(children, node), nodeClass)))
            }
        }
    else
        object : ParseNext<T> {
            override fun next(pos: Int, node: T): ParseContState {
                return parseRepeatLoop(sub, addChain(children, node), count-1, len+node.len, ctx, pos, startPos, next, nodeClass, type)
            }
        }
    )
}

fun <T : Node, R : Node> repeat (sub : Parser<T>, min : Int, max : Int, nodeClass: Class<T>, type : (Int, Int, Array<T>) -> R) : Parser<R> {
    assert (min <= max)

    return object : Parser<R> () {
        override fun parseInternal(ctx: ParseContext, start: Int, next: ParseNext<R>): ParseContState {
            if (max == 0) {
                return next.next(start, type (start, 0, java.lang.reflect.Array.newInstance(nodeClass, 0) as Array<T>))
            } else {
                ctx.branchQueue.add(fun (ctxi : ParseContext) : ParseContState {
                    var res = ParseContState.FAIL
                    if (min == 0) {
                        when (next.next(start, type (start, 0, java.lang.reflect.Array.newInstance(nodeClass, 0) as Array<T>))) {
                            ParseContState.OK -> return ParseContState.OK
                            ParseContState.SUSPEND -> res = ParseContState.SUSPEND
                            else -> {}
                        }
                    }

                    for (count in (if (min == 0) 1 else min) .. max) {
                        when (parseRepeatLoop(sub, null, count, 0, ctx, start, start, next, nodeClass, type)) {
                            ParseContState.SUSPEND -> res = ParseContState.SUSPEND
                            ParseContState.OK -> return ParseContState.OK
                            ParseContState.FAIL -> {}
                        }
                    }

                    return res;
                })
                return ParseContState.SUSPEND
            }
        }
    }
}

inline fun <reified T : Node, R : Node> repeat (sub : Parser<T>, min : Int, max : Int, noinline type : (Int, Int, Array<T>) -> R) : Parser<R> =
    repeat<T, R>(sub, min, max, T::class.java, type);

fun repeat (sub : Parser<Node>, min : Int, max : Int) : Parser<Sequence> =
    repeat<Node, Sequence>(sub, min, max, Node::class.java, type=::Sequence);
