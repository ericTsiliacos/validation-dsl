package validation

internal fun <T> validateEachList(
    list: List<T>,
    path: String,
    block: FieldValidationScope<T>.() -> Unit
): Validated<Unit> {
    val results = list.mapIndexed { index, item ->
        val itemPath = "$path[$index]"
        FieldValidationScope(itemPath) { item }.apply(block).evaluate()
    }
    return combineResults(*results.toTypedArray()).map { }
}

