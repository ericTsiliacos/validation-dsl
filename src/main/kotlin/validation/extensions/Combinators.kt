package validation.extensions

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
 * Composes two rules and evaluates them independently.
 *
 * Both rules are executed regardless of whether the first one passes or fails,
 * and any resulting errors are combined.
 *
 * Example:
 * ```kotlin
 * val notBlank = fromPredicate("username", "must not be blank") { it.isNotBlank() }
 * val isLowercase = fromPredicate("username", "must be lowercase") { it == it.lowercase() }
 *
 * val combined = notBlank combine isLowercase
 * ```
 */
infix fun <T> Rule<T>.combine(other: Rule<T>): Rule<T> = { value ->
    val r1 = this(value)
    val r2 = other(value)
    combineResults(r1, r2).map { }
}

/**
 * Wraps an existing rule function that returns a [Validated] result.
 *
 * Useful for composing or adapting low-level rule logic into the standard [Rule] type.
 */
fun <T> fromFunction(rule: (T) -> Validated<Unit>): Rule<T> = rule
