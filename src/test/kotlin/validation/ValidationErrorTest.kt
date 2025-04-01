package validation

import org.testng.AssertJUnit.*
import org.testng.annotations.Test

class ValidationErrorTest {

    @Test
    fun `ValidationResult isValid returns true when no errors`() {
        val result = ValidationResult(emptyList())
        assertTrue(result.isValid)
    }

    @Test
    fun `ValidationResult isValid returns false when there are errors`() {
        val result = ValidationResult(listOf(ValidationError("field", "must not be blank")))
        assertFalse(result.isValid)
    }

    @Test
    fun `ValidationError contains path and message`() {
        val error = ValidationError("name", "must not be blank")
        assertEquals("name", error.path)
        assertEquals("must not be blank", error.message)
    }

    @Test
    fun `from returns valid result when Validated is Valid`() {
        val validated: Validated<Unit> = Validated.Valid(Unit)
        val result = ValidationResult.from(validated)
        assertTrue(result.isValid)
    }

    @Test
    fun `from returns invalid result when Validated is Invalid`() {
        val error = ValidationError("field", "error")
        val validated: Validated<Unit> = Validated.Invalid(listOf(error))
        val result = ValidationResult.from(validated)
        assertFalse(result.isValid)
        assertEquals(listOf(error), result.errors)
    }

    @Test
    fun `fromMany flattens errors from multiple validated results`() {
        val e1 = ValidationError("field1", "bad")
        val e2 = ValidationError("field2", "also bad")

        val validatedList = listOf(
            Validated.Valid(Unit),
            Validated.Invalid(listOf(e1)),
            Validated.Invalid(listOf(e2))
        )

        val result = ValidationResult.fromMany(validatedList)
        assertFalse(result.isValid)
        assertEquals(listOf(e1, e2), result.errors)
    }

}
