package validation.extensions

import org.testng.AssertJUnit
import org.testng.annotations.Test
import validation.core.*

class CombinatorsTest {

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
            AssertJUnit.assertEquals(1, errors.size)
            errors[0].assertMatches("x", "must be lowercase")
        }

        combined("A").assertInvalid { errors ->
            errors[0].assertMatches("x", "must be longer than 3")
            errors[1].assertMatches("x", "must be lowercase")
        }
    }

    @Test
    fun `fromFunction returns valid when rule succeeds`() {
        val rule = fromFunction<String> {
            Validated.Valid(Unit)
        }
        rule("any input").assertValid()
    }

    @Test
    fun `fromFunction handles null safely if rule allows`() {
        val rule = fromFunction<String?> {
            if (it == null) Validated.Invalid(listOf(ValidationError("field", "must not be null")))
            else Validated.Valid(Unit)
        }

        rule(null).assertInvalid { errors ->
            errors[0].assertMatches("field", "must not be null")
        }
    }

    @Test
    fun `fromFunction supports reusable rule objects`() {
        val reusableRule: Rule<String> = {
            if (it.startsWith("a")) Validated.Valid(Unit)
            else Validated.Invalid(listOf(ValidationError("value", "must start with 'a'")))
        }

        val rule1 = fromFunction(reusableRule)
        val rule2 = fromFunction(reusableRule)

        rule1("abc").assertValid()
        rule2("xyz").assertInvalid { errors ->
            errors[0].assertMatches("value", "must start with 'a'")
        }
    }

    @Test
    fun `fromFunction wraps raw rule correctly`() {
        val rawRule: Rule<String> = { input ->
            if (input.length > 3) Validated.Valid(Unit)
            else Validated.Invalid(listOf(ValidationError("field", "too short")))
        }

        val rule = fromFunction(rawRule)

        rule("okay").assertValid()
        rule("no").assertInvalid { errors ->
            errors[0].assertMatches("field", "too short")
        }
    }

}