package validation

import validation.RuleBuilder.ValidationNode
import kotlin.reflect.KProperty1

class ValidationScope<R>(
    private val path: String,
    private val getter: () -> R
) {
    private val rootRules = mutableListOf<ValidationNode.Rule<R>>()
    private val nestedValidators = mutableListOf<(R) -> List<ValidationError>>()

    fun rule(message: String, predicate: (R) -> Boolean): RuleBuilder<R> {
        val rule = ValidationNode.Rule(message, predicate)
        rootRules += rule
        return RuleBuilder(rule, this)
    }

    fun <T> ValidationScope<T?>.whenNotNull(
        block: ValidationScope<T>.() -> Unit
    ) {
        nestedValidators += { parentNullable ->
            if (parentNullable != null) {
                ValidationScope<T>(path) { parentNullable }.apply(block).evaluate()
            } else emptyList()
        }
    }

    fun <E> validate(
        prop: KProperty1<R, E>,
        block: ValidationScope<E>.() -> Unit
    ) {
        val subPath = combinePath(path, prop.name)
        nestedValidators += { parent ->
            val value = prop.get(parent)
            ValidationScope(subPath) { value }.apply(block).evaluate()
        }
    }

    fun <E> validateEach(
        prop: KProperty1<R, List<E>>,
        block: ValidationScope<E>.() -> Unit
    ) {
        val listPath = combinePath(path, prop.name)
        nestedValidators += { parent ->
            val list = prop.get(parent)
            validateEachList(list, listPath, block)
        }
    }

    fun evaluate(): List<ValidationError> {
        val value = getter()
        val ruleErrors = evaluateNodes(value, rootRules)
        val nestedErrors = nestedValidators.flatMap { it(value) }
        return ruleErrors + nestedErrors
    }

    private fun evaluateNodes(value: R, nodes: List<ValidationNode.Rule<R>>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        for (node in nodes) {
            if (!node.predicate(value)) {
                errors += ValidationError(path, node.message)
            } else {
                errors += evaluateNodes(value, node.children)
            }
        }
        return errors
    }

    private fun combinePath(parent: String, child: String): String =
        if (parent.isEmpty()) child else "$parent.$child"

}
