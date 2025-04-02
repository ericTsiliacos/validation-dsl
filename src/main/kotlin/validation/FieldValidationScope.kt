package validation

import kotlin.reflect.KProperty1

@ValidationDsl
class FieldValidationScope<R>(
    internal val path: String,
    internal val getter: () -> R
) {
    private val rules = mutableListOf<Rule<R>>()
    internal val nested: MutableList<() -> Validated<Unit>> = mutableListOf()

    fun rule(message: String, predicate: (R) -> Boolean): RuleBuilder<R> {
        val rule = Rules.fromPredicate(path = path, message = message, predicate = predicate)
        rules += rule
        return RuleBuilder(rule)
    }

    fun rule(rule: Rule<R>) {
        rules += rule
    }

    fun <E> validate(
        prop: KProperty1<R, E>,
        block: FieldValidationScope<E>.() -> Unit
    ) {
        val subPath = combinePath(path, prop.name)
        nested += {
            val value = prop.get(getter())
            FieldValidationScope(subPath, { value }).apply(block).evaluate()
        }
    }

    fun <E> validateEach(
        prop: KProperty1<R, List<E>>,
        block: FieldValidationScope<E>.() -> Unit
    ) {
        val listPath = combinePath(path, prop.name)
        nested += {
            val list = prop.get(getter())
            val results = list.mapIndexed { index, item ->
                val itemPath = "$listPath[$index]"
                FieldValidationScope(itemPath) { item }.apply(block).evaluate()
            }
            combineResults(*results.toTypedArray()).map { }
        }
    }

    fun evaluate(): Validated<Unit> {
        val value = getter()

        val ruleResults = rules.map { it(value) }
        val nestedResults = nested.map { it() }

        val allResults = ruleResults + nestedResults
        return combineResults(*allResults.toTypedArray()).map { }
    }

    private fun combinePath(parent: String, child: String): String =
        if (parent.isEmpty()) child else "$parent.$child"

}
