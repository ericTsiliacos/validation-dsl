package validation.core

import org.testng.Assert.assertNull
import org.testng.annotations.Test

class RuleChainScopeTest {

    @Test
    fun `build returns null when no rules are added`() {
        val scope = RuleChainScope<String>(PropertyPath("username"))
        val result = scope.build()
        assertNull(result)
    }

    @Test
    fun `build returns single rule when only one is added`() {
        val scope = RuleChainScope<String>(PropertyPath("username"))
        scope.rule("must not be blank") { it.isNotBlank() }

        val rule = scope.build()
        requireNotNull(rule)
        rule("").assertInvalid { errors ->
            errors[0].assertMatches("username", "must not be blank")
        }
    }

    @Test
    fun `build returns chained rule that short-circuits on failure`() {
        val scope = RuleChainScope<String>(PropertyPath("username"))
        scope.rule("must not be blank") { it.isNotBlank() }
        scope.rule("must be lowercase") { it == it.lowercase() }

        val rule = scope.build()
        requireNotNull(rule)
        rule("").assertInvalid { errors ->
            errors[0].assertMatches("username", "must not be blank")
        }
    }

    @Test
    fun `build returns chained rule that evaluates all if prior rules pass`() {
        val scope = RuleChainScope<String>(PropertyPath("username"))
        scope.rule("must not be blank") { it.isNotBlank() }
        scope.rule("must be lowercase") { it == it.lowercase() }

        val rule = scope.build()
        requireNotNull(rule)
        rule("Valid").assertInvalid { errors ->
            errors[0].assertMatches("username", "must be lowercase")
        }
    }

    @Test
    fun `chained rule returns valid if all pass`() {
        val scope = RuleChainScope<String>(PropertyPath("username"))
        scope.rule("must not be blank") { it.isNotBlank() }
        scope.rule("must be lowercase") { it == it.lowercase() }

        val rule = scope.build()
        requireNotNull(rule)
        rule("valid").assertValid()
    }

}
