package validation.dsl

import validation.core.*

/**
 * Entry point for creating a [Validator] using a fluent DSL.
 *
 * Example:
 * ```
 * val validator = validator<User> {
 *     validate(User::name) {
 *         rule("must not be blank") { it.isNotBlank() }
 *     }
 * }
 * ```
 *
 * This allows structuring validations declaratively, rule-by-rule.
 */
fun <T> validator(block: Validator<T>.() -> Unit): Validator<T> =
    Validator<T>().apply(block)

/**
 * Creates a reusable, path-agnostic validation rule.
 */
fun <T> rule(
    message: String,
    code: String? = null,
    group: String? = null,
    predicate: (T) -> Boolean
): Rule<T> = { value ->
    if (predicate(value)) {
        Validated.Valid(Unit)
    } else {
        Validated.Invalid(
            listOf(ValidationError(path = "", message = message, code = code, group = group))
        )
    }
}

/**
 * Applies a reusable, path-agnostic [Rule] within the current validation context.
 *
 * This function is intended for use within a [FieldValidationScope], where the field `path`
 * is known and can be injected automatically into any [ValidationError]s produced by the rule.
 *
 * Reusable rules defined outside the DSL (e.g., via [rule]) typically omit the `path` so they
 * can be reused across different validation contexts. This function ensures that the correct
 * path is applied at the point of usage.
 *
 * Example:
 * ```
 * val notBlank = rule<String>("must not be blank") { it.isNotBlank() }
 *
 * validate(User::name) {
 *     use(notBlank) // injects "name" as the error path
 * }
 * ```
 *
 * @param rule A reusable validation rule defined without a path.
 */
@ValidationDsl
fun <T> FieldValidationScope<T>.use(rule: Rule<T>) {
    val wrapped: Rule<T> = { value ->
        val result = rule(value)
        if (result is Validated.Invalid) {
            Validated.Invalid(result.errors.map { err ->
                if (err.path.isBlank()) err.copy(path = path) else err
            })
        } else result
    }

    this@use.rule(wrapped)
}

/**
 * Executes the [block] only if the current value is non-null.
 *
 * This is useful for conditionally applying rules to optional fields:
 * ```
 * validate(User::nickname) {
 *     whenNotNull {
 *         rule("must be at least 3 characters") { it.length >= 3 }
 *     }
 * }
 * ```
 */
@ValidationDsl
fun <T : Any> FieldValidationScope<T?>.whenNotNull(
    block: FieldValidationScope<T>.() -> Unit
) {
    this.nested += {
        val value = this.getter()
        if (value != null) {
            FieldValidationScope(this.path) { value }.apply(block).evaluate()
        } else {
            Validated.Valid(Unit)
        }
    }
}

/**
 * Validates each item in a list individually and accumulates all errors.
 *
 * Example:
 * ```
 * validate(User::tags) {
 *     validateEach {
 *         rule("must not be blank") { it.isNotBlank() }
 *     }
 * }
 * ```
 *
 * Produces paths like `tags[0]`, `tags[1]`, etc.
 */
@ValidationDsl
fun <T> FieldValidationScope<List<T>>.validateEach(
    block: FieldValidationScope<T>.() -> Unit
) {
    this.nested += {
        val parentList = this.getter()
        val results = parentList.mapIndexed { index, item ->
            val itemPath = "${this.path}[$index]"
            FieldValidationScope(itemPath) { item }.apply(block).evaluate()
        }
        combineResults(*results.toTypedArray()).map { }
    }
}

/**
 * Adds a chain of dependent validation rules that short-circuit on the first failure.
 *
 * Each rule is only evaluated if the previous rule passed. This is ideal when later rules
 * assume successful conditions from earlier ones (e.g., type safety, parsing, etc.).
 *
 * Example:
 * ```
 * validate(User::ageStr) {
 *     dependent {
 *         rule("must be numeric") { it.all(Char::isDigit) }
 *         rule("must be ≥ 18") { it.toInt() >= 18 }
 *     }
 * }
 * ```
 *
 * If no rules are defined, nothing is added.
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

/**
 * Groups related validation rules under a label.
 *
 * All validation errors produced directly within this block will receive the given [label]
 * as their `group` field. Nested `validate`, `validateEach`, `dependent`, or nested `group`
 * blocks will **not** inherit this label unless explicitly grouped themselves.
 *
 * This improves organization, traceability, and display of validation errors.
 *
 * Example:
 * ```
 * validate(User::name) {
 *     group("basic checks") {
 *         rule("must not be blank") { it.isNotBlank() }
 *         rule("must be at least 3 characters") { it.length >= 3 }
 *     }
 * }
 * ```
 *
 * @param label A logical name for organizing related rules (e.g., "identity", "address checks").
 */
@ValidationDsl
fun <T> FieldValidationScope<T>.group(
    label: String,
    block: FieldValidationScope<T>.() -> Unit
) {
    this.nested += {
        val scoped = FieldValidationScope(path, getter).apply(block).evaluate()
        if (scoped is Validated.Invalid) {
            Validated.Invalid(scoped.errors.map { it.copy(group = label) })
        } else scoped
    }
}
