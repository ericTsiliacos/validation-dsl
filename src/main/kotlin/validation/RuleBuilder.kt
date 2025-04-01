package validation

import validation.Rules.andThen

class RuleBuilder<R>(
    private var rule: Rule<R>
) {

    fun andThen(message: String, predicate: (R) -> Boolean): RuleBuilder<R> {
        val chained = Rules.fromPredicate("", message, predicate)
        rule = rule andThen chained
        return this
    }

    fun build(): Rule<R> = rule

}

