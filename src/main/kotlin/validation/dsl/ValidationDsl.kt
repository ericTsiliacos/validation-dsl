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
 * Applies an existing [Validator] to the current field value.
 *
 * This is useful for delegating validation of nested types to reusable,
 * modular validators — especially when they contain conditional logic or dependencies.
 *
 * Example:
 * ```
 * validate(User::profile) {
 *     use(profileValidator(countryService))
 * }
 * ```
 *
 * All errors returned from the nested validator will be scoped to the current
 * property path (e.g., `profile.bio`, `profile.birthday`).
 *
 * This can be composed with [whenNotNull], [group], and other DSL features.
 *
 * @param validator The validator to apply to the current field value
 */
@ValidationDsl
fun <T> FieldValidationScope<T>.use(validator: Validator<T>) {
    nested += {
        when (val result = validator.validate(getter())) {
            is ValidationResult.Valid -> Validated.Valid(Unit)
            is ValidationResult.Invalid -> Validated.Invalid(
                result.errors.map { error ->
                    error.copy(path = path.child(error.path.toString()))
                }
            )
        }
    }
}

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

@ValidationDsl
fun <T : Any> FieldValidationScope<T?>.ruleIfPresent(
    message: String,
    code: String? = null,
    group: String? = null,
    predicate: (T) -> Boolean
) {
    whenNotNull {
        rule(message, code, group, predicate)
    }
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

/**
 * Injects current path into reusable predicate.
 */
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

/**
 * DSL sugar for inline rule definition inside validation blocks.
 */
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
            val itemPath = this.path.index(index)
            FieldValidationScope(itemPath) { item }.apply(block).evaluate()
        }
        combineResults(*results.toTypedArray()).toUnit()
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
