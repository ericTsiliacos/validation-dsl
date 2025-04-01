package validation

import org.testng.AssertJUnit.*
import org.testng.annotations.Test
import validation.Rules.andThen
import validation.Rules.combine

class RuleTest {

    @Test
    fun `rule from predicate passes and fails correctly`() {
        val rule: Rule<String> = Rules.fromPredicate("username", "must not be blank") {
            it.isNotBlank()
        }

        val pass = rule("hello")
        val fail = rule("")

        assertTrue(pass is Validated.Valid)
        assertTrue(fail is Validated.Invalid)
        val errors = (fail as Validated.Invalid).errors
        assertEquals("username", errors[0].path)
        assertEquals("must not be blank", errors[0].message)
    }

    @Test
    fun `andThen chains dependent rules and short-circuits on failure`() {
        val rule1 = Rules.fromPredicate<String>("age", "must be numeric") { it.all { c -> c.isDigit() } }
        val rule2 = Rules.fromPredicate<String>("age", "must be >= 18") { it.toInt() >= 18 }
        val chained = rule1 andThen rule2

        val valid = chained("21")
        assertTrue(valid is Validated.Valid)

        val invalidNumeric = chained("abc")
        assertTrue(invalidNumeric is Validated.Invalid)
        val errors = (invalidNumeric as Validated.Invalid).errors
        assertEquals(1, errors.size)
        assertEquals("must be numeric", errors[0].message)

        val invalidTooYoung = chained("17")
        assertTrue(invalidTooYoung is Validated.Invalid)
        assertEquals("must be >= 18", (invalidTooYoung as Validated.Invalid).errors[0].message)
    }

    @Test
    fun `combine applies both rules and accumulates errors`() {
        val r1 = Rules.fromPredicate<String>("username", "must be longer than 3") {
            it.length > 3
        }
        val r2 = Rules.fromPredicate<String>("username", "must be lowercase") {
            it == it.lowercase()
        }

        val combined = r1 combine r2
        val result = combined("A")

        assertTrue(result is Validated.Invalid)
        val errors = (result as Validated.Invalid).errors

        assertEquals(2, errors.size)
        assertEquals("must be longer than 3", errors[0].message)
        assertEquals("must be lowercase", errors[1].message)
    }

}
