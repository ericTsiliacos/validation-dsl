package validation

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
 *     validateEachItem {
 *         rule("must not be blank") { it.isNotBlank() }
 *     }
 * }
 * ```
 *
 * Produces paths like `tags[0]`, `tags[1]`, etc.
 */
fun <T> FieldValidationScope<List<T>>.validateEachItem(
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
fun <T> FieldValidationScope<T>.dependent(
    block: RuleChainScope<T>.() -> Unit
) {
    val chain = RuleChainScope<T>(this.path).apply(block).build()
    if (chain != null) {
        this.rule(chain)
    }
}

/**
 * Composes two rules into a short-circuiting sequence.
 *
 * If the first rule passes, the second rule is evaluated.
 * Otherwise, the first rule's error is returned immediately.
 *
 * Example:
 * ```kotlin
 * val numeric = Rules.fromPredicate<String>("age", "must be numeric") { it.all(Char::isDigit) }
 * val over18 = Rules.fromPredicate<String>("age", "must be ≥ 18") { it.toInt() >= 18 }
 *
 * val ageRule = numeric andThen over18
 *
 * validate(User::age) {
 *     rule(ageRule)
 * }
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
 * This is useful when validations are independent but should be grouped together.
 *
 * Example:
 * ```kotlin
 * val notBlank = Rules.fromPredicate("username", "must not be blank") { it.isNotBlank() }
 * val isLowercase = Rules.fromPredicate("username", "must be lowercase") { it == it.lowercase() }
 *
 * val combined = notBlank combine isLowercase
 *
 * validate(User::username) {
 *     rule(combined)
 * }
 * ```
 */
infix fun <T> Rule<T>.combine(other: Rule<T>): Rule<T> = { value ->
    val r1 = this(value)
    val r2 = other(value)
    combineResults(r1, r2).map { }
}

/**
 * Creates a rule from a simple predicate function.
 *
 * If the [predicate] returns false, the rule fails with the given [message], [path], and optional [code].
 *
 * This is the most common way to define field validations.
 *
 * Example:
 * ```kotlin
 * val rule = fromPredicate("email", "must contain @") { it.contains("@") }
 * ```
 */
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

/**
 * Wraps an existing rule function that returns a [Validated] result.
 *
 * Useful for composing or adapting low-level rule logic into the standard [Rule] type.
 *
 * Example:
 * ```kotlin
 * val custom: Rule<String> = { if (it.length > 5) Validated.Valid(Unit) else Validated.Invalid(...) }
 * val rule = fromFunction(custom)
 * ```
 */
fun <T> fromFunction(rule: (T) -> Validated<Unit>): Rule<T> = rule

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
