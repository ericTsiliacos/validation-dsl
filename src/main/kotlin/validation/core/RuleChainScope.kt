package validation.core

import validation.dsl.ValidationDsl
import validation.extensions.andThen

@ValidationDsl
class RuleChainScope<T>(private val path: String) {
    private var currentRule: Rule<T>? = null

    fun rule(message: String, predicate: (T) -> Boolean) {
        val nextRule = fromPredicate(path = path, message = message, predicate = predicate)
        currentRule = currentRule?.andThen(nextRule) ?: nextRule
    }

    fun build(): Rule<T>? = currentRule
}
