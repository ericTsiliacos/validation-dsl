package validation

import org.testng.AssertJUnit.*
import org.testng.annotations.Test

class ValidationDslTest {

    data class User(val name: String?, val tags: List<String>)

    @Test
    fun `validator builds and runs validation`() {
        val v = validator {
            validate(User::name) {
                rule("must not be null") { it != null }
            }
        }

        val result = v.validate(User(null, emptyList()))
        assertFalse(result.isValid)
        assertEquals("must not be null", result.errors[0].message)
    }

    @Test
    fun `whenNotNull skips rule for null`() {
        val v = validator {
            validate(User::name) {
                whenNotNull {
                    rule("must be at least 3 chars") { it.length >= 3 }
                }
            }
        }

        val result = v.validate(User(null, emptyList()))
        assertTrue(result.isValid)
    }

    @Test
    fun `whenNotNull runs rule for non-null`() {
        val v = validator {
            validate(User::name) {
                whenNotNull {
                    rule("must be at least 3 chars") { it.length >= 3 }
                }
            }
        }

        val result = v.validate(User("ab", emptyList()))
        assertFalse(result.isValid)
        assertEquals("must be at least 3 chars", result.errors[0].message)
    }

    @Test
    fun `validateEachItem validates each item in list`() {
        val scope = FieldValidationScope("tags") { listOf("", "ok", " ") }

        scope.validateEachItem {
            rule("must not be blank") { it.isNotBlank() }
        }

        val result = scope.evaluate()

        assertTrue(result is Validated.Invalid)
        val errors = (result as Validated.Invalid).errors

        assertEquals(2, errors.size)
        assertEquals("tags[0]", errors[0].path)
        assertEquals("must not be blank", errors[0].message)
        assertEquals("tags[2]", errors[1].path)
    }

    @Test
    fun `dependent rules short-circuit on first failure`() {
        val scope = FieldValidationScope("age") { "abc" }

        scope.dependent {
            rule("must be numeric") { it.all(Char::isDigit) }
            rule("must be ≥ 18") { it.toInt() >= 18 }
        }

        val result = scope.evaluate()
        val errors = (result as Validated.Invalid).errors

        assertEquals(1, errors.size)
        assertEquals("must be numeric", errors[0].message)
    }

    @Test
    fun `dependent rules continue if first passes`() {
        val scope = FieldValidationScope("age") { "15" }

        scope.dependent {
            rule("must be numeric") { it.all(Char::isDigit) }
            rule("must be ≥ 18") { it.toInt() >= 18 }
        }

        val result = scope.evaluate()
        val errors = (result as Validated.Invalid).errors

        assertEquals(1, errors.size)
        assertEquals("must be ≥ 18", errors[0].message)
    }

    @Test
    fun `dependent block with one rule works`() {
        val scope = FieldValidationScope("username") { "" }

        scope.dependent {
            rule("must not be blank") { it.isNotBlank() }
        }

        val result = scope.evaluate()
        val errors = (result as Validated.Invalid).errors

        assertEquals(1, errors.size)
        assertEquals("must not be blank", errors[0].message)
    }

    @Test
    fun `dependent block with zero rules does nothing`() {
        val scope = FieldValidationScope("noop") { "ok" }

        scope.dependent {}

        val result = scope.evaluate()
        assertTrue(result is Validated.Valid)
    }

}
