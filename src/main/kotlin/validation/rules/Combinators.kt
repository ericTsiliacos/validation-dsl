package validation.rules

import validation.core.*

/**
 * Composes two rules into a short-circuiting sequence.
 *
 * If the first rule passes, the second rule is evaluated.
 * Otherwise, the first rule's error is returned immediately.
 *
 * Example:
 * ```kotlin
 * val numeric = fromPredicate("age", "must be numeric") { it.all(Char::isDigit) }
 * val over18 = fromPredicate("age", "must be ≥ 18") { it.toInt() >= 18 }
 *
 * val ageRule = numeric andThen over18
 * ```
 */
infix fun <T> Rule<T>.andThen(next: Rule<T>): Rule<T> = { value ->
    this(value).flatMap { next(value) }
}

/**
 * Wraps an existing rule function that returns a [Validated] result.
 *
 * Useful for composing or adapting low-level rule logic into the standard [Rule] type.
 */
fun <T> fromFunction(rule: (T) -> Validated<Unit>): Rule<T> = rule

infix fun <T> Rule<T>.and(other: Rule<T>): Rule<T> = { value ->
    val first = this(value)
    val second = other(value)

    when {
        first is Validated.Valid && second is Validated.Valid ->
            Validated.Valid(Unit)
        first is Validated.Invalid && second is Validated.Invalid ->
            Validated.Invalid(first.errors + second.errors)
        first is Validated.Invalid -> first
        else -> second
    }
}

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
