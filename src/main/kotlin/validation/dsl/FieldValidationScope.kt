package validation.dsl

import validation.core.*
import kotlin.reflect.KProperty1

@ValidationDsl
class FieldValidationScope<R>(
    internal val path: PropertyPath,
    internal val root: () -> R
) {
    internal val rules = mutableListOf<Rule<R>>()
    internal val nested: MutableList<() -> Validated<Unit>> = mutableListOf()

    val value: R get() = root()

    fun <E> validate(
        prop: KProperty1<R, E>,
        block: FieldValidationScope<E>.() -> Unit
    ) {
        val subPath = path.child(prop.name)
        nested += {
            val value = prop.get(root())
            FieldValidationScope(subPath) { value }.apply(block).evaluate()
        }
    }

    fun <E> validateEach(
        prop: KProperty1<R, List<E>>,
        block: FieldValidationScope<E>.() -> Unit
    ) {
        val listPath = path.child(prop.name)
        nested += {
            val list = prop.get(root())
            val results = list.mapIndexed { index, item ->
                val itemPath = listPath.index(index)
                FieldValidationScope(itemPath) { item }.apply(block).evaluate()
            }
            combineResults(*results.toTypedArray()).toUnit()
        }
    }

    fun evaluate(): Validated<Unit> {
        val value = root()

        val ruleResults = rules.map { it(value) }
        val nestedResults = nested.map { it() }

        val allResults = ruleResults + nestedResults
        return combineResults(*allResults.toTypedArray()).toUnit()
    }

}
