package validation.core

import org.testng.AssertJUnit.*
import org.testng.annotations.Test

class ValidationResultTest {

    @Test
    fun `ValidationResult isValid returns true when no errors`() {
        val result = ValidationResult.from("value", emptyList())
        assertTrue(result.isValid())
        assertEquals("value", result.getOrNull())
    }

    @Test
    fun `ValidationResult isValid returns false when there are errors`() {
        val error = ValidationError(PropertyPath("field"), "must not be blank")
        val result = ValidationResult.from("value", listOf(error))
        assertFalse(result.isValid())
        assertEquals(listOf(error), (result as ValidationResult.Invalid).errors)
    }

    @Test
    fun `ValidationError contains path and message`() {
        val error = ValidationError(PropertyPath("name"), "must not be blank")
        error.assertMatches("name", "must not be blank")
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

        val errors = validatedList.flatMap {
            when (it) {
                is Validated.Valid -> emptyList()
                is Validated.Invalid -> it.errors
            }
        }

        val result = ValidationResult.fromMany(errors)

        assertFalse(result.isValid())
        assertEquals(listOf(e1, e2), (result as ValidationResult.Invalid).errors)
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

    @Test
    fun `ValidationResult map transforms value when valid`() {
        val result = ValidationResult.Valid("foo")
        val mapped = result.map { it.uppercase() }
        assertEquals("FOO", mapped.getOrNull())
    }

    @Test
    fun `ValidationResult map does not run when invalid`() {
        val error = ValidationError(PropertyPath("x"), "bad")
        val result = ValidationResult.from("ignored", listOf(error))
        var called = false
        val mapped = result.map { called = true; it.length }
        assertFalse(called)
        assertTrue(mapped is ValidationResult.Invalid)
        assertEquals(listOf(error), (mapped as ValidationResult.Invalid).errors)
    }

    @Test
    fun `ValidationResult flatMap chains valid results`() {
        val result = ValidationResult.Valid("foo")
        val mapped = result.flatMap { ValidationResult.Valid(it.length) }
        assertEquals(3, mapped.getOrNull())
    }

    @Test
    fun `ValidationResult flatMap does not run when invalid`() {
        val error = ValidationError(PropertyPath("x"), "bad")
        val result = ValidationResult.from("x", listOf(error))
        val mapped = result.flatMap { ValidationResult.Valid(it.length) }
        assertTrue(mapped is ValidationResult.Invalid)
        assertEquals(listOf(error), (mapped as ValidationResult.Invalid).errors)
    }

    @Test
    fun `getOrElse returns value when valid`() {
        val result = ValidationResult.Valid("actual")
        val value = result.getOrElse { "default" }
        assertEquals("actual", value)
    }

    @Test
    fun `getOrElse returns default when invalid`() {
        val error = ValidationError(PropertyPath("x"), "bad")
        val result = ValidationResult.from("ignored", listOf(error))
        val value = result.getOrElse { "default" }
        assertEquals("default", value)
    }

    @Test
    fun `onValid executes block when valid`() {
        var captured: String? = null
        val result = ValidationResult.Valid("run this")
        result.onValid { captured = it }
        assertEquals("run this", captured)
    }

    @Test
    fun `onValid does not execute block when invalid`() {
        var wasCalled = false
        val error = ValidationError(PropertyPath("x"), "bad")
        val result = ValidationResult.from("input", listOf(error))
        result.onValid { wasCalled = true }
        assertFalse(wasCalled)
    }

    @Test
    fun `ValidationError root helper builds error with empty path`() {
        val error = ValidationError.root("boom", code = "C", group = "g")
        assertEquals(PropertyPath.root(), error.path)
        assertEquals("boom", error.message)
        assertEquals("C", error.code)
        assertEquals("g", error.group)
    }

    @Test
    fun `ValidationResult isInvalid reports true for invalid result`() {
        val error = ValidationError(PropertyPath("field"), "bad")
        val result = ValidationResult.from("x", listOf(error))
        assertTrue(result.isInvalid())
        assertFalse(result.isValid())
    }

    @Test
    fun `onInvalid executes block when result is invalid`() {
        val error = ValidationError(PropertyPath("f"), "bad")
        val result = ValidationResult.from("x", listOf(error))
        var captured: List<ValidationError>? = null
        val returned = result.onInvalid { captured = it }
        assertSame(result, returned)
        assertEquals(listOf(error), captured)
    }

}
