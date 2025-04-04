package validation

@ValidationDsl
class RuleBuilder<R>(
    private var rule: Rule<R>
) {

    fun andThen(message: String, predicate: (R) -> Boolean): RuleBuilder<R> {
        val chained = fromPredicate(path = "", message = message, predicate = predicate)
        rule = rule andThen chained
        return this
    }

    fun build(): Rule<R> = rule

}
