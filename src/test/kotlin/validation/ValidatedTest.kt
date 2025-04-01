package validation

import org.testng.AssertJUnit.*
import org.testng.annotations.Test

class ValidatedTest {

    @Test
    fun `map transforms value when valid`() {
        val result = Validated.Valid(2).map { it * 2 }
        assertTrue(result is Validated.Valid)
        assertEquals(4, (result as Validated.Valid).value)
    }

    @Test
    fun `map returns same invalid when not valid`() {
        val error = ValidationError("field", "invalid")
        val result = Validated.Invalid(listOf(error)).map { 42 }
        assertTrue(result is Validated.Invalid)
        assertEquals(listOf(error), (result as Validated.Invalid).errors)
    }

    @Test
    fun `flatMap transforms when valid`() {
        val result = Validated.Valid(2).flatMap { Validated.Valid(it * 3) }
        assertTrue(result is Validated.Valid)
        assertEquals(6, (result as Validated.Valid).value)
    }

    @Test
    fun `flatMap skips mapping when invalid`() {
        val error = ValidationError("x", "oops")
        val result = Validated.Invalid(listOf(error)).flatMap { Validated.Valid(999) }
        assertTrue(result is Validated.Invalid)
        assertEquals(listOf(error), (result as Validated.Invalid).errors)
    }

    @Test
    fun `ap applies function when both valid`() {
        val f: Validated<(Int) -> String> = Validated.Valid { it.toString() + "!" }
        val x: Validated<Int> = Validated.Valid(5)

        val result = f.ap(x)
        assertTrue(result is Validated.Valid)
        assertEquals("5!", (result as Validated.Valid).value)
    }

    @Test
    fun `ap accumulates errors when both invalid`() {
        val e1 = ValidationError("a", "bad")
        val e2 = ValidationError("b", "also bad")

        val f: Validated<(Int) -> String> = Validated.Invalid(listOf(e1))
        val x: Validated<Int> = Validated.Invalid(listOf(e2))

        val result = f.ap(x)
        assertTrue(result is Validated.Invalid)
        assertEquals(listOf(e1, e2), (result as Validated.Invalid).errors)
    }

    @Test
    fun `ap returns left error if only function is invalid`() {
        val e = ValidationError("f", "bad function")
        val f: Validated<(Int) -> String> = Validated.Invalid(listOf(e))
        val x: Validated<Int> = Validated.Valid(1)

        val result = f.ap(x)
        assertTrue(result is Validated.Invalid)
        assertEquals(listOf(e), (result as Validated.Invalid).errors)
    }

    @Test
    fun `ap returns right error if only argument is invalid`() {
        val e = ValidationError("x", "bad input")
        val f = Validated.Valid { x: Int -> x * 2 }
        val x = Validated.Invalid(listOf(e))

        val result = f.ap(x)
        assertTrue(result is Validated.Invalid)
        assertEquals(listOf(e), (result as Validated.Invalid).errors)
    }

    @Test
    fun `combineResults returns all valid values if no errors`() {
        val result = combineResults(
            Validated.Valid("a"),
            Validated.Valid("b"),
            Validated.Valid("c")
        )

        assertTrue(result is Validated.Valid)
        assertEquals(listOf("a", "b", "c"), (result as Validated.Valid).value)
    }

    @Test
    fun `combineResults returns all errors if any invalid`() {
        val e1 = ValidationError("one", "bad 1")
        val e2 = ValidationError("two", "bad 2")

        val result = combineResults(
            Validated.Valid("good"),
            Validated.Invalid(listOf(e1)),
            Validated.Invalid(listOf(e2))
        )

        assertTrue(result is Validated.Invalid)
        assertEquals(listOf(e1, e2), (result as Validated.Invalid).errors)
    }

    @Test
    fun `combineResults returns empty list when no inputs`() {
        val result = combineResults<String>()

        assertTrue(result is Validated.Valid)
        assertEquals(emptyList<String>(), (result as Validated.Valid).value)
    }

    @Test
    fun `ap with function that returns null still wraps in Validated`() {
        val f: Validated<(Int) -> String?> = Validated.Valid { null }
        val x: Validated<Int> = Validated.Valid(1)

        val result = f.ap(x)
        assertTrue(result is Validated.Valid)
        assertNull((result as Validated.Valid).value)
    }

    @Test
    fun `map can produce null values safely`() {
        val result = Validated.Valid("hello").map { null }
        assertTrue(result is Validated.Valid)
        assertNull((result as Validated.Valid).value)
    }

    @Test
    fun `flatMap with function that returns Invalid still works`() {
        val error = ValidationError("fail", "broken")
        val result = Validated.Valid("x").flatMap { Validated.Invalid(listOf(error)) }

        assertTrue(result is Validated.Invalid)
        assertEquals(listOf(error), (result as Validated.Invalid).errors)
    }
}
