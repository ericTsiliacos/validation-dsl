package validation.dsl.scopes

import validation.core.*

/**
 * Creates a reusable, path-agnostic validation rule.
 */
fun <T> rule(
    message: String,
    code: String? = null,
    group: String? = null,
    predicate: (T) -> Boolean
): Rule<T> = { value ->
    if (predicate(value)) Validated.Valid(Unit)
    else Validated.Invalid(listOf(ValidationError.root(message = message, code = code, group = group)))
}

/**
 * DSL entry point for defining reusable, path-agnostic rules.
 */
fun <T> predicate(
    message: String,
    code: String? = null,
    group: String? = null,
    check: (T) -> Boolean
): Rule<T> = rule(message, code, group, check)

/** Injects current path into reusable predicate. */
@ValidationDsl
fun <T> FieldValidationScope<T>.rule(rule: Rule<T>) {
    val wrapped: Rule<T> = { value ->
        when (val result = rule(value)) {
            is Validated.Valid -> result
            is Validated.Invalid -> {
                val updatedErrors = result.errors.map { it.copy(path = path) }
                Validated.Invalid(updatedErrors)
            }
        }.toUnit()
    }
    this.rules += wrapped
}

/** DSL sugar for inline rule definition inside validation blocks. */
@ValidationDsl
fun <T> FieldValidationScope<T>.rule(
    message: String,
    code: String? = null,
    group: String? = null,
    check: (T) -> Boolean
) {
    val predicate = predicate(message, code, group, check)
    rule(predicate)
}

/**
 * Adds a chain of dependent validation rules that short-circuit on the first failure.
 */
@ValidationDsl
fun <T> FieldValidationScope<T>.dependent(
    block: RuleChainScope<T>.() -> Unit
) {
    val chain = RuleChainScope<T>(this.path).apply(block).build()
    if (chain != null) {
        this.rule(chain)
    }
}
