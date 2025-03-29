package validation

import validation.RuleBuilder.ValidationNode
import validation.RuleBuilder.ValidationNode.PredicateRule
import kotlin.reflect.KProperty1

class FieldValidationScope<R>(
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

    fun rule(rule: PredicateRule<R>): RuleBuilder<R> = rule(rule.message, rule.predicate)

    fun <T> FieldValidationScope<T?>.whenNotNull(
        block: FieldValidationScope<T>.() -> Unit
    ) {
        nestedValidators += { parentNullable ->
            if (parentNullable != null) {
                FieldValidationScope<T>(path) { parentNullable }.apply(block).evaluate()
            } else emptyList()
        }
    }

    fun <E> validate(
        prop: KProperty1<R, E>,
        block: FieldValidationScope<E>.() -> Unit
    ) {
        val subPath = combinePath(path, prop.name)
        nestedValidators += { parent ->
            val value = prop.get(parent)
            FieldValidationScope(subPath) { value }.apply(block).evaluate()
        }
    }

    fun <E> validateEach(
        prop: KProperty1<R, List<E>>,
        block: FieldValidationScope<E>.() -> Unit
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

    private fun evaluateNodes(value: R, rootNodes: List<ValidationNode.Rule<R>>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val stack = ArrayDeque<Pair<ValidationNode.Rule<R>, Boolean>>()

        rootNodes.forEach { stack.addLast(it to true) }

        while (stack.isNotEmpty()) {
            val (node, parentPassed) = stack.removeLast()

            if (!parentPassed || !node.predicate(value)) {
                errors += ValidationError(path, node.message)
            } else {
                node.children.forEach { child ->
                    stack.addLast(child to true)
                }
            }
        }

        return errors
    }

    private fun combinePath(parent: String, child: String): String =
        if (parent.isEmpty()) child else "$parent.$child"

}
