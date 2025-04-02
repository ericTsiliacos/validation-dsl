package validation

import org.testng.AssertJUnit.*
import org.testng.annotations.Test
import validation.Rules.andThen
import validation.Rules.combine
import validation.Rules.fromPredicate

class RulesTest {

    @Test
    fun `fromPredicate returns Valid when predicate passes`() {
        val rule = fromPredicate<String>("field", "must not be blank") { it.isNotBlank() }
        rule("abc").assertValid()
    }

    @Test
    fun `fromPredicate returns Invalid when predicate fails`() {
        val rule = fromPredicate<String>("field", "must not be blank") { it.isNotBlank() }
        rule("").assertInvalid { errors ->
            errors[0].assertMatches("field", "must not be blank")
        }
    }

    @Test
    fun `fromPredicate includes code when provided`() {
        val rule = fromPredicate<String>(
            path = "field",
            message = "must not be blank",
            code = "error.blank"
        ) { it.isNotBlank() }

        rule("").assertInvalid { errors ->
            errors[0].assertMatches("field", "must not be blank", code = "error.blank")
        }
    }

    @Test
    fun `andThen short-circuits if first rule fails`() {
        val rule1 = fromPredicate<String>("x", "must not be empty") { it.isNotEmpty() }
        val rule2 = fromPredicate<String>("x", "must be lowercase") { it == it.lowercase() }

        val combined = rule1 andThen rule2

        combined("").assertInvalid { errors ->
            errors[0].assertMatches("x", "must not be empty")
        }
    }

    @Test
    fun `andThen runs second rule only if first passes`() {
        val rule1 = fromPredicate<String>("x", "must not be empty") { it.isNotEmpty() }
        val rule2 = fromPredicate<String>("x", "must be lowercase") { it == it.lowercase() }

        val combined = rule1 andThen rule2

        combined("Hello").assertInvalid { errors ->
            errors[0].assertMatches("x", "must be lowercase")
        }
    }

    @Test
    fun `combine accumulates both errors`() {
        val rule1 = fromPredicate<String>("x", "must be longer than 3") { it.length > 3 }
        val rule2 = fromPredicate<String>("x", "must be lowercase") { it == it.lowercase() }

        val combined = rule1 combine rule2

        combined("Hello").assertInvalid { errors ->
            assertEquals(1, errors.size)
            errors[0].assertMatches("x", "must be lowercase")
        }

        combined("A").assertInvalid { errors ->
            errors[0].assertMatches("x", "must be longer than 3")
            errors[1].assertMatches("x", "must be lowercase")
        }
    }

}
