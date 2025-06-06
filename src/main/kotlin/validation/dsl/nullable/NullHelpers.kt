package validation.dsl.nullable

import validation.core.Validated
import validation.dsl.scopes.FieldValidationScope
import validation.dsl.scopes.rule as scopeRule
import validation.dsl.ValidationDsl

@ValidationDsl
fun <T : Any> FieldValidationScope<T?>.whenNotNull(
    block: FieldValidationScope<T>.() -> Unit
) {
    this.nested += {
        val value = this.root()
        if (value != null) {
            FieldValidationScope(this.path) { value }.apply(block).evaluate()
        } else {
            Validated.Valid(Unit)
        }
    }
}

@ValidationDsl
fun <T : Any> FieldValidationScope<T?>.ruleIfPresent(
    message: String,
    code: String? = null,
    group: String? = null,
    predicate: (T) -> Boolean
) {
    whenNotNull {
        scopeRule(message, code, group, predicate)
    }
}
