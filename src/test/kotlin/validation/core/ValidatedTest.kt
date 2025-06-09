package validation.core

import org.testng.AssertJUnit.*
import org.testng.annotations.Test

class ValidatedTest {

    @Test
    fun `map transforms value when valid`() {
        val result = Validated.Valid(2).map { it * 2 }
        result.assertValid()
        assertEquals(4, (result as Validated.Valid).value)
    }

    @Test
    fun `map returns same invalid when not valid`() {
        val error = ValidationError(PropertyPath("field"), "invalid")
        val result = Validated.Invalid(listOf(error)).map { 42 }
        result.assertInvalid { errors ->
            assertEquals(listOf(error), errors)
        }
    }

    @Test
    fun `flatMap transforms when valid`() {
        val result = Validated.Valid(2).flatMap { Validated.Valid(it * 3) }
        result.assertValid()
        assertEquals(6, (result as Validated.Valid).value)
    }

    @Test
    fun `flatMap skips mapping when invalid`() {
        val error = ValidationError(PropertyPath("x"), "oops")
        val result = Validated.Invalid(listOf(error)).flatMap { Validated.Valid(999) }
        result.assertInvalid { errors -> assertEquals(listOf(error), errors) }
    }

    @Test
    fun `ap applies function when both valid`() {
        val f: Validated<(Int) -> String> = Validated.Valid { x -> "$x!" }
        val x = Validated.Valid(5)
        val result = f.ap(x)
        result.assertValid()
        assertEquals("5!", (result as Validated.Valid).value)
    }

    @Test
    fun `ap accumulates errors when both invalid`() {
        val e1 = ValidationError(PropertyPath("a"), "bad")
        val e2 = ValidationError(PropertyPath("b"), "also bad")
        val f: Validated<(Int) -> String> = Validated.Invalid(listOf(e1))
        val x = Validated.Invalid(listOf(e2))

        val result = f.ap(x)
        result.assertInvalid { errors -> assertEquals(listOf(e1, e2), errors) }
    }

    @Test
    fun `ap returns left error if only function is invalid`() {
        val e = ValidationError(PropertyPath("f"), "bad function")
        val f: Validated<(Int) -> Int> = Validated.Invalid(listOf(e))
        val x = Validated.Valid(1)

        val result = f.ap(x)
        result.assertInvalid { errors -> assertEquals(listOf(e), errors) }
    }

    @Test
    fun `ap returns right error if only argument is invalid`() {
        val e = ValidationError(PropertyPath("x"), "bad input")
        val f: Validated<(Int) -> Int> = Validated.Valid { x -> x * 2 }
        val x = Validated.Invalid(listOf(e))

        val result = f.ap(x)
        result.assertInvalid { errors -> assertEquals(listOf(e), errors) }
    }

    @Test
    fun `combineResults returns all valid values if no errors`() {
        val result = combineResultsFromList(
            listOf(
                Validated.Valid("a"),
                Validated.Valid("b"),
                Validated.Valid("c")
            )
        )

        result.assertValid()
        assertEquals(listOf("a", "b", "c"), (result as Validated.Valid).value)
    }

    @Test
    fun `combineResults returns all errors if any invalid`() {
        val e1 = ValidationError(PropertyPath("one"), "bad 1")
        val e2 = ValidationError(PropertyPath("two"), "bad 2")

        val result = combineResultsFromList(
            listOf(
                Validated.Valid("good"),
                Validated.Invalid(listOf(e1)),
                Validated.Invalid(listOf(e2))
            )
        )

        result.assertInvalid { errors -> assertEquals(listOf(e1, e2), errors) }
    }

    @Test
    fun `combineResults returns empty list when no inputs`() {
        val result = combineResultsFromList<String>(emptyList())
        result.assertValid()
        assertEquals(emptyList<String>(), (result as Validated.Valid).value)
    }

    @Test
    fun `ap with function that returns null still wraps in Validated`() {
        val f: Validated<(Int) -> String?> = Validated.Valid { _: Int -> null }
        val x = Validated.Valid(1)

        val result = f.ap(x)
        result.assertValid()
        assertNull((result as Validated.Valid).value)
    }

    @Test
    fun `map can produce null values safely`() {
        val result = Validated.Valid("hello").map { null }
        result.assertValid()
        assertNull((result as Validated.Valid).value)
    }

    @Test
    fun `flatMap with function that returns Invalid still works`() {
        val error = ValidationError(PropertyPath("fail"), "broken")
        val result = Validated.Valid("x").flatMap { Validated.Invalid(listOf(error)) }
        result.assertInvalid { errors -> assertEquals(listOf(error), errors) }
    }

}
