package validation.dsl.grouping

import validation.core.Validated
import validation.dsl.ValidationDsl
import validation.dsl.scopes.FieldValidationScope

@ValidationDsl
fun <T> FieldValidationScope<T>.group(
    label: String,
    block: FieldValidationScope<T>.() -> Unit
) {
    this.nested += {
        val scoped = FieldValidationScope(path, root).apply(block).evaluate()
        if (scoped is Validated.Invalid) {
            Validated.Invalid(scoped.errors.map { it.copy(group = label) })
        } else scoped
    }
}
