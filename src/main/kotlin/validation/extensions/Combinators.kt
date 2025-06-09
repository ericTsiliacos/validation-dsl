package validation.extensions

import validation.core.*

fun <T> Rule<T>.isForbidden(
    message: String,
    code: String? = null,
    group: String? = null
): Rule<T> = { value ->
    when (this(value)) {
        is Validated.Valid -> Validated.Invalid(
            listOf(ValidationError(PropertyPath.EMPTY, message, code, group))
        )
        is Validated.Invalid -> Validated.Valid(Unit)
    }
}
