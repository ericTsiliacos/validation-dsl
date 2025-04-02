package validation

import org.testng.AssertJUnit.*
import org.testng.annotations.Test
import validation.Rules.andThen
import validation.Rules.combine

class RulesTest {

    @Test
    fun `fromPredicate returns Valid when predicate passes`() {
        val rule = Rules.fromPredicate<String>(
            path = "field",
            message = "must not be blank"
        ) { it.isNotBlank() }

        val result = rule("abc")
        assertTrue(result is Validated.Valid)
    }

    @Test
    fun `fromPredicate returns Invalid when predicate fails`() {
        val rule = Rules.fromPredicate<String>(
            path = "field",
            message = "must not be blank"
        ) { it.isNotBlank() }

        val result = rule("")
        assertTrue(result is Validated.Invalid)
        val error = (result as Validated.Invalid).errors.first()
        assertEquals("field", error.path)
        assertEquals("must not be blank", error.message)
        assertNull(error.code)
    }

    @Test
    fun `fromPredicate includes code when provided`() {
        val rule = Rules.fromPredicate<String>(
            path = "field",
            message = "must not be blank",
            code = "error.blank"
        ) { it.isNotBlank() }

        val result = rule("")
        val error = (result as Validated.Invalid).errors.first()
        assertEquals("error.blank", error.code)
    }

    @Test
    fun `andThen short-circuits if first rule fails`() {
        val rule1 = Rules.fromPredicate<String>("x", "must not be empty") { it.isNotEmpty() }
        val rule2 = Rules.fromPredicate<String>("x", "must be lowercase") { it == it.lowercase() }

        val combined = rule1 andThen rule2

        val result = combined("")
        val error = (result as Validated.Invalid).errors.first()
        assertEquals("must not be empty", error.message)
    }

    @Test
    fun `andThen runs second rule only if first passes`() {
        val rule1 = Rules.fromPredicate<String>("x", "must not be empty") { it.isNotEmpty() }
        val rule2 = Rules.fromPredicate<String>("x", "must be lowercase") { it == it.lowercase() }

        val combined = rule1 andThen rule2

        val result = combined("Hello")
        val error = (result as Validated.Invalid).errors.first()
        assertEquals("must be lowercase", error.message)
    }

    @Test
    fun `combine accumulates both errors`() {
        val rule1 = Rules.fromPredicate<String>("x", "must be longer than 3") { it.length > 3 }
        val rule2 = Rules.fromPredicate<String>("x", "must be lowercase") { it == it.lowercase() }

        val combined = rule1 combine rule2

        val result = combined("Hello")
        assertTrue(result is Validated.Invalid)

        val errors = (result as Validated.Invalid).errors
        assertEquals(1, errors.size)
        assertEquals("must be lowercase", errors[0].message)

        val bothFail = combined("A")
        val bothErrors = (bothFail as Validated.Invalid).errors
        assertEquals(2, bothErrors.size)
        assertEquals("must be longer than 3", bothErrors[0].message)
        assertEquals("must be lowercase", bothErrors[1].message)
    }

}
