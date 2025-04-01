package validation

import validation.Rules.andThen

@ValidationDsl
class RuleChainScope<T>(private val path: String) {
    private var currentRule: Rule<T>? = null

    fun rule(message: String, predicate: (T) -> Boolean) {
        val nextRule = Rules.fromPredicate(path, message, predicate)
        currentRule = currentRule?.andThen(nextRule) ?: nextRule
    }

    fun build(): Rule<T>? = currentRule
}
