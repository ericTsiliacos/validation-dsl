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

}
