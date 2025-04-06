package validation.extensions

import org.testng.AssertJUnit.*
import org.testng.annotations.Test
import validation.core.*

class CombinatorsTest {

    private val isRed: Rule<String> =
        fromPredicate(PropertyPath("color"), "must be red") { it == "red" }

    private val lengthOver3 = fromPredicate<String>(
        PropertyPath("input"),
        message = "must be longer than 3"
    ) { it.length > 3 }

    private val startsWithA = fromPredicate<String>(
        PropertyPath("input"),
        message = "must start with A"
    ) { it.startsWith("A") }

    @Test
    fun `andThen short-circuits if first rule fails`() {
        val rule1 = fromPredicate<String>(PropertyPath("x"), "must not be empty") { it.isNotEmpty() }
        val rule2 = fromPredicate<String>(PropertyPath("x"), "must be lowercase") { it == it.lowercase() }

        val combined = rule1 andThen rule2

        combined("").assertInvalid { errors ->
            errors[0].assertMatches("x", "must not be empty")
        }
    }

    @Test
    fun `andThen runs second rule only if first passes`() {
        val rule1 = fromPredicate<String>(PropertyPath("x"), "must not be empty") { it.isNotEmpty() }
        val rule2 = fromPredicate<String>(PropertyPath("x"), "must be lowercase") { it == it.lowercase() }

        val combined = rule1 andThen rule2

        combined("Hello").assertInvalid { errors ->
            errors[0].assertMatches("x", "must be lowercase")
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
            if (it == null) Validated.Invalid(listOf(ValidationError(PropertyPath("field"), "must not be null")))
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
            else Validated.Invalid(listOf(ValidationError(PropertyPath("value"), "must start with 'a'")))
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
            else Validated.Invalid(listOf(ValidationError(PropertyPath("field"), "too short")))
        }

        val rule = fromFunction(rawRule)

        rule("okay").assertValid()
        rule("no").assertInvalid { errors ->
            errors[0].assertMatches("field", "too short")
        }
    }

    @Test
    fun `original rule should pass when value is red`() {
        val result = isRed("red")
        assertTrue(result.isValid())
    }

    @Test
    fun `original rule should fail when value is blue`() {
        val result = isRed("blue")
        assertFalse(result.isValid())
        val errors = (result as Validated.Invalid).errors
        assertEquals("must be red", errors.first().message)
        assertEquals(PropertyPath("color"), errors.first().path)
    }

    @Test
    fun `isForbidden should fail when original rule passes`() {
        val forbidden = isRed.isForbidden("Red is not allowed", code = "RED_ERROR")
        val result = forbidden("red")

        assertFalse(result.isValid())
        val error = (result as Validated.Invalid).errors.first()
        assertEquals("Red is not allowed", error.message)
        assertEquals("RED_ERROR", error.code)
        assertEquals(PropertyPath.EMPTY, error.path) // because we override it
    }

    @Test
    fun `isForbidden should pass when original rule fails`() {
        val forbidden = isRed.isForbidden("Red is not allowed")
        val result = forbidden("blue")

        assertTrue(result.isValid())
    }

    @Test
    fun `isForbidden does not invert predicate, only result`() {
        val isNotRed = isRed.isForbidden("Red is not allowed")
        val redResult = isNotRed("red")
        val blueResult = isNotRed("blue")

        assertFalse(redResult.isValid())
        assertTrue(blueResult.isValid())
    }

    @Test
    fun `passes when both predicates pass`() {
        val result = (lengthOver3 and startsWithA)("Apple")
        assertTrue(result.isValid())
    }

    @Test
    fun `fails when length fails but start passes`() {
        val result = (lengthOver3 and startsWithA)("A")
        assertFalse(result.isValid())
        val errors = (result as Validated.Invalid).errors
        assertEquals(1, errors.size)
        assertEquals("must be longer than 3", errors.first().message)
    }

    @Test
    fun `fails when start fails but length passes`() {
        val result = (lengthOver3 and startsWithA)("hello")
        assertFalse(result.isValid())
        val errors = (result as Validated.Invalid).errors
        assertEquals(1, errors.size)
        assertEquals("must start with A", errors.first().message)
    }

    @Test
    fun `fails when both predicates fail`() {
        val result = (lengthOver3 and startsWithA)("Hi")
        assertFalse(result.isValid())
        val errors = (result as Validated.Invalid).errors
        assertEquals(2, errors.size)
        assertEquals(setOf("must be longer than 3", "must start with A"), errors.map { it.message }.toSet())
    }

}
