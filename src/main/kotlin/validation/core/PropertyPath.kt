package validation.core

@JvmInline
value class PropertyPath(private val segments: List<String>) {

    companion object {
        val EMPTY = PropertyPath(emptyList())
        fun root() = EMPTY
    }

    constructor(root: String) : this(listOf(root))

    fun child(name: String): PropertyPath = PropertyPath(segments + name)

    fun index(i: Int): PropertyPath = PropertyPath(segments + "[$i]")

    override fun toString(): String = buildString {
        segments.forEachIndexed { i, seg ->
            if (seg.startsWith("[")) append(seg) else {
                if (i != 0) append(".")
                append(seg)
            }
        }
    }

} 
