package validation

import org.testng.AssertJUnit.*
import org.testng.annotations.Test

class RuleChainScopeTest {

    @Test
    fun `build returns null when no rules are added`() {
        val scope = RuleChainScope<String>("username")
        val result = scope.build()
        assertNull(result)
    }

    @Test
    fun `build returns single rule when only one is added`() {
        val scope = RuleChainScope<String>("username")
        scope.rule("must not be blank") { it.isNotBlank() }

        val rule = scope.build()
        assertNotNull(rule)

        val result = rule!!("")
        assertTrue(result is Validated.Invalid)
        assertEquals("must not be blank", (result as Validated.Invalid).errors[0].message)
    }

    @Test
    fun `build returns chained rule that short-circuits on failure`() {
        val scope = RuleChainScope<String>("username")

        scope.rule("must not be blank") { it.isNotBlank() }
        scope.rule("must be lowercase") { it == it.lowercase() }

        val rule = scope.build()
        assertNotNull(rule)

        val result = rule!!("") // fails the first rule
        assertTrue(result is Validated.Invalid)
        assertEquals("must not be blank", (result as Validated.Invalid).errors[0].message)
    }

    @Test
    fun `build returns chained rule that evaluates all if prior rules pass`() {
        val scope = RuleChainScope<String>("username")

        scope.rule("must not be blank") { it.isNotBlank() }
        scope.rule("must be lowercase") { it == it.lowercase() }

        val rule = scope.build()
        assertNotNull(rule)

        val result = rule!!("Valid")
        assertTrue(result is Validated.Invalid)
        assertEquals("must be lowercase", (result as Validated.Invalid).errors[0].message)
    }

    @Test
    fun `chained rule returns valid if all pass`() {
        val scope = RuleChainScope<String>("username")

        scope.rule("must not be blank") { it.isNotBlank() }
        scope.rule("must be lowercase") { it == it.lowercase() }

        val rule = scope.build()
        val result = rule!!("valid")

        assertTrue(result is Validated.Valid)
    }

}
