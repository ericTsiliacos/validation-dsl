package validation

internal fun <T> validateEachList(
    list: List<T>,
    path: String,
    block: FieldValidationScope<T>.() -> Unit
): List<ValidationError> {
    return list.asSequence().flatMapIndexed { index, item ->
        val itemPath = "$path[$index]"
        FieldValidationScope(itemPath) { item }.apply(block).evaluate()
    }.toList()
}
