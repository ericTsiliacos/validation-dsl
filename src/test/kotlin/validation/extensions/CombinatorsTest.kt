package validation.extensions

import org.testng.AssertJUnit.*
import org.testng.annotations.Test
import validation.core.*
import validation.dsl.predicate

class CombinatorsTest {

    private val isRed: Rule<String> = predicate("must be red") { it == "red" }

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
        assertEquals(PropertyPath.EMPTY, errors.first().path)
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

}
