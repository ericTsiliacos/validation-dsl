package validation.dsl

import validation.core.PropertyPath
import validation.core.Rule
import validation.core.Validated
import validation.core.ValidationError
import validation.extensions.andThen

@ValidationDsl
class RuleChainScope<T>(private val path: PropertyPath) {
    private var currentRule: Rule<T>? = null

    fun rule(message: String, predicate: (T) -> Boolean) {
        val nextRule = { value: T ->
            if (predicate(value)) {
                Validated.Valid(Unit)
            } else {
                Validated.Invalid(
                    listOf(ValidationError(path = path, message = message))
                )
            }
        }
        currentRule = currentRule?.andThen(nextRule) ?: nextRule
    }

    fun build(): Rule<T>? = currentRule

}
