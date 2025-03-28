package validation

internal fun <T> validateEachList(
    list: List<T>,
    path: String,
    block: ValidationScope<T>.() -> Unit
): List<ValidationError> {
    return list.asSequence().flatMapIndexed { index, item ->
        val itemPath = "$path[$index]"
        ValidationScope(itemPath) { item }.apply(block).evaluate()
    }.toList()
}
