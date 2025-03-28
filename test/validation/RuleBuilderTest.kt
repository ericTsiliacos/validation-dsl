package validation

import org.testng.AssertJUnit.*
import org.testng.annotations.Test

class ValidationRuleTest {

    @Test
    fun `ValidationNode Rule stores message and predicate`() {
        val rule = RuleBuilder.ValidationNode.Rule<String>("must not be blank", { it.isNotBlank() })

        assertEquals("must not be blank", rule.message)
        assertTrue(rule.predicate("hello"))
        assertFalse(rule.predicate(""))
    }

    @Test
    fun `RuleBuilder can chain with andThen`() {
        val root = RuleBuilder.ValidationNode.Rule<String>("not null", { true })
        val scope = ValidationScope("dummy") { "value" }

        val builder = RuleBuilder(root, scope)
        builder.andThen("must be lowercase") { it == it.lowercase() }

        assertEquals(1, root.children.size)
        assertEquals("must be lowercase", root.children[0].message)
    }

}
