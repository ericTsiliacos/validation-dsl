package validation.core

import org.testng.AssertJUnit.*
import org.testng.annotations.Test

class ValidationResultTest {

    @Test
    fun `ValidationResult isValid returns true when no errors`() {
        val result = ValidationResult(emptyList())
        assertTrue(result.isValid)
    }

    @Test
    fun `ValidationResult isValid returns false when there are errors`() {
        val result = ValidationResult(listOf(ValidationError(PropertyPath("field"), "must not be blank")))
        assertFalse(result.isValid)
    }

    @Test
    fun `ValidationError contains path and message`() {
        val error = ValidationError(PropertyPath("name"), "must not be blank")
        error.assertMatches("name", "must not be blank")
    }

    @Test
    fun `from returns valid result when Validated is Valid`() {
        val validated: Validated<Unit> = Validated.Valid(Unit)
        val result = ValidationResult.from(validated)
        assertTrue(result.isValid)
    }

    @Test
    fun `from returns invalid result when Validated is Invalid`() {
        val error = ValidationError(PropertyPath("field"), "error")
        val validated: Validated<Unit> = Validated.Invalid(listOf(error))
        val result = ValidationResult.from(validated)
        assertFalse(result.isValid)
        assertEquals(listOf(error), result.errors)
    }

    @Test
    fun `fromMany flattens errors from multiple validated results`() {
        val e1 = ValidationError(PropertyPath("field1"), "bad")
        val e2 = ValidationError(PropertyPath("field2"), "also bad")

        val validatedList = listOf(
            Validated.Valid(Unit),
            Validated.Invalid(listOf(e1)),
            Validated.Invalid(listOf(e2))
        )

        val result = ValidationResult.fromMany(validatedList)
        assertFalse(result.isValid)
        assertEquals(listOf(e1, e2), result.errors)
    }

    @Test
    fun `ValidationError code is null by default`() {
        val error = ValidationError(PropertyPath("email"), "must not be blank")
        assertNull(error.code)
    }

    @Test
    fun `ValidationError stores code when provided`() {
        val error = ValidationError(PropertyPath("email"), "must not be blank", code = "email.blank")
        error.assertMatches("email", "must not be blank", code = "email.blank")
    }

}
