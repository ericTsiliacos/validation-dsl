package validation

import kotlin.reflect.KProperty1

class FieldValidationScope<R>(
    private val path: String,
    private val getter: () -> R
) {
    private val rules = mutableListOf<Rule<R>>()
    private val nested: MutableList<() -> Validated<Unit>> = mutableListOf()

    fun rule(message: String, predicate: (R) -> Boolean): RuleBuilder<R> {
        val rule = Rules.fromPredicate(path, message, predicate)
        rules += rule
        return RuleBuilder(rule)
    }

    fun rule(rule: Rule<R>) {
        rules += rule
    }

    private fun <T : Any> whenNotNull(
        parentScope: FieldValidationScope<T?>,
        block: FieldValidationScope<T>.() -> Unit
    ): () -> Validated<Unit> {
        return {
            val value = parentScope.getter()
            if (value != null) {
                FieldValidationScope(parentScope.path) { value }.apply(block).evaluate()
            } else {
                Validated.Valid(Unit)
            }
        }
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
            combineResults(
                *list.mapIndexed { index, item ->
                    val itemPath = "$listPath[$index]"
                    FieldValidationScope(itemPath, { item }).apply(block).evaluate()
                }.toTypedArray()
            ).map { Unit }
        }
    }

    fun evaluate(): Validated<Unit> {
        val value = getter()

        val ruleResults = rules.map { it(value) }
        val nestedResults = nested.map { it() }

        val allResults = ruleResults + nestedResults
        return combineResults(*allResults.toTypedArray()).map { Unit }
    }

    fun <T : Any> FieldValidationScope<T?>.whenNotNull(block: FieldValidationScope<T>.() -> Unit) {
        nested += whenNotNull(this, block)
    }

    private fun combinePath(parent: String, child: String): String =
        if (parent.isEmpty()) child else "$parent.$child"

}
