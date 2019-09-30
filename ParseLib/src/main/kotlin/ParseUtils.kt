package ParseLib

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

internal data class Chain <T> (val obj : T, val count : Int, val next : Chain<T>?)

internal inline fun <reified T> toReverseArray (c : Chain<T>?) : Array<T> {
    return if (c == null)
        arrayOf()
    else {
        val res = arrayOfNulls<T>(c.count)
        var scan = c

        while (scan != null) {
            res[scan.count - 1] = scan.obj
            scan = scan.next
        }

        @Suppress("UNCHECKED_CAST")
        res as Array<T>
    }
}

internal fun <T> addChain (c : Chain<T>?, obj : T) : Chain<T> {
    return Chain(obj, (c?.count ?: 0)+1, c)
}

internal enum class ParseContState { OK, FAIL, SUSPEND }