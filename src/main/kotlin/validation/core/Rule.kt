package validation.core

typealias Rule<T> = (T) -> Validated<Unit>

fun <T> fromPredicate(
    path: String,
    message: String,
    code: String? = null,
    predicate: (T) -> Boolean
): Rule<T> = { value ->
    if (predicate(value)) {
        Validated.Valid(Unit)
    } else {
        Validated.Invalid(
            listOf(ValidationError(path, message, code))
        )
    }
}
