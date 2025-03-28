import kotlin.reflect.KProperty1

data class ValidationError(val path: String, val message: String)

class ValidationResult(val errors: List<ValidationError>) {
    val isValid: Boolean get() = errors.isEmpty()
}

sealed class ValidationNode<R> {
    class Rule<R>(
        val message: String,
        val predicate: (R) -> Boolean,
        val children: MutableList<Rule<R>> = mutableListOf()
    ) : ValidationNode<R>()
}

class RuleBuilder<R>(
    private val node: ValidationNode.Rule<R>,
    private val scope: FieldValidationScope<R>
) {
    fun andThen(message: String, predicate: (R) -> Boolean): RuleBuilder<R> {
        val child = ValidationNode.Rule(message, predicate)
        node.children += child
        return RuleBuilder(child, scope)
    }
}

private fun <T> validateEachList(
    list: List<T>,
    path: String,
    block: FieldValidationScope<T>.() -> Unit
): List<ValidationError> {
    return list.asSequence().flatMapIndexed { index, item ->
        val itemPath = "$path[$index]"
        FieldValidationScope(itemPath) { item }.apply(block).evaluate()
    }.toList()
}

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

class Validator<T> {
    private val rules = mutableListOf<(T) -> List<ValidationError>>()

    fun <R> validate(
        prop: KProperty1<T, R>,
        block: FieldValidationScope<R>.() -> Unit
    ) {
        val path = prop.name
        rules += { target ->
            val value = prop.get(target)
            FieldValidationScope(path) { value }.apply(block).evaluate()
        }
    }

    fun <R> validateEach(
        prop: KProperty1<T, List<R>>,
        block: FieldValidationScope<R>.() -> Unit
    ) {
        val path = prop.name
        rules += { target ->
            val list = prop.get(target)
            validateEachList(list, path, block)
        }
    }

    fun validate(target: T): ValidationResult {
        return ValidationResult(rules.flatMap { it(target) })
    }
}

fun <T> validator(block: Validator<T>.() -> Unit): Validator<T> =
    Validator<T>().apply(block)
