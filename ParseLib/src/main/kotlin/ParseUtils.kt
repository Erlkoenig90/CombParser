// Helpers for parsing

package ParseLib

fun indent(depth : Int) =
        "\t".repeat(depth)

abstract class Node (val start : Int, val len : Int) {
    abstract fun print (str : String, depth : Int = 0) : String
    fun matched (str : String) = str.substring (start, start+len)

    override fun equals(other: Any?): Boolean =
        other is Node && other.start == start && other.len == len

    override fun hashCode(): Int = 31 * start + len
}

open class Terminal (start : Int, len : Int) : Node (start, len) {
    override fun print(str : String, depth : Int) = indent(depth) + "Terminal(" + matched(str) + ")"

    override fun equals(other: Any?): Boolean =
            other is Terminal && super.equals(other)

    override fun hashCode(): Int = 31 * start + len
}

open class Sequence (start : Int, len : Int, val children : Array<Node>) : Node (start, len) {
    override fun print(str: String, depth : Int): String {
        return indent(depth) + "Sequence(\n" + printChildren(str, depth) + indent(depth) + ")"
    }
    fun printChildren(str : String, depth : Int) =
            children.joinToString(",\n") { c -> c.print(str, depth + 1) } + "\n"

    override fun equals(other: Any?): Boolean =
            other is Sequence && super.equals(other) && children.contentEquals(other.children)

    override fun hashCode(): Int = 31 * super.hashCode() + children.contentHashCode()
}

internal data class Chain <T> (val obj : T, val count : Int, val next : Chain<T>?)

internal fun <T> toReverseArray (c : Chain<T>?, klass : Class<T>) : Array<T> {
    @Suppress("UNCHECKED_CAST")
    val res = java.lang.reflect.Array.newInstance(klass, c?.count ?: 0) as Array<T>
    var scan = c

    while (scan != null) {
        res[scan.count - 1] = scan.obj
        scan = scan.next
    }

    return res
}

internal inline fun <reified T> toReverseArray (c : Chain<T>?) : Array<T> {
    return toReverseArray(c, T::class.java)
}

internal fun <T> addChain (c : Chain<T>?, obj : T) : Chain<T> {
    return Chain(obj, (c?.count ?: 0)+1, c)
}

internal enum class ParseContState { OK, FAIL, SUSPEND }