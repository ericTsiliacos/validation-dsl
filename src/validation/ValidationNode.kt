package validation

class RuleBuilder<R>(
    private val node: ValidationNode.Rule<R>,
    private val scope: ValidationScope<R>
) {

    sealed class ValidationNode<R> {
        class Rule<R>(
            val message: String,
            val predicate: (R) -> Boolean,
            val children: MutableList<Rule<R>> = mutableListOf()
        ) : ValidationNode<R>()
    }

    fun andThen(message: String, predicate: (R) -> Boolean): RuleBuilder<R> {
        val child = ValidationNode.Rule(message, predicate)
        node.children += child
        return RuleBuilder(child, scope)
    }

}
