package validation

/**
 * Entry point for creating a Validator in a DSL-style.
 */
fun <T> validator(block: Validator<T>.() -> Unit): Validator<T> =
    Validator<T>().apply(block)

/**
 * Only runs the block if the nullable value is not null.
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
 * Validates each item in a list and accumulates errors.
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
 * Adds a chain of dependent validation rules that are evaluated sequentially.
 *
 * Each rule in the [block] is only evaluated if the previous rule passes.
 * This enables short-circuiting behavior for validations that depend on earlier conditions.
 *
 * For example, this can be used to ensure that a string is numeric **before**
 * applying numeric comparisons:
 *
 * ```
 * validate(User::age) {
 *     dependent {
 *         rule("must be numeric") { it.all(Char::isDigit) }
 *         rule("must be ≥ 18") { it.toInt() >= 18 }
 *     }
 * }
 * ```
 *
 * If any rule in the chain fails, the subsequent rules are skipped, and
 * only the first failing rule's error is returned.
 *
 * If no rules are defined in the [block], nothing is added.
 */
fun <T> FieldValidationScope<T>.dependent(
    block: RuleChainScope<T>.() -> Unit
) {
    val chain = RuleChainScope<T>(this.path).apply(block).build()
    if (chain != null) {
        this.rule(chain)
    }
}
