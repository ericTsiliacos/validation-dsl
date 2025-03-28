package validation

import org.testng.AssertJUnit.assertEquals
import org.testng.AssertJUnit.assertTrue
import org.testng.annotations.Test
import validation.validator

class ValidatorTest {

    data class User(val name: String, val tags: List<Tag>)
    data class Tag(val value: String)

    @Test
    fun `validate registers field rules`() {
        val validator = Validator<User>()
        validator.validate(User::name) {
            rule("must not be blank") { it.isNotBlank() }
        }

        val result = validator.validate(User(name = "", tags = emptyList()))
        assertEquals(1, result.errors.size)
        assertEquals("name", result.errors[0].path)
    }

    @Test
    fun `validateEach applies rules to list elements`() {
        val validator = Validator<User>()
        validator.validateEach(User::tags) {
            validate(Tag::value) {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        val result = validator.validate(User(name = "ok", tags = listOf(Tag(""), Tag("x"))))
        assertEquals(1, result.errors.size)
        assertEquals("tags[0].value", result.errors[0].path)
    }

    @Test
    fun `validator function builds Validator instance`() {
        val validator = validator {
            validate(User::name) {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        val result = validator.validate(User(name = "", tags = emptyList()))
        assertEquals(1, result.errors.size)
        assertEquals("name", result.errors[0].path)
    }

    @Test
    fun `validate aggregates multiple errors`() {
        val validator = validator {
            validate(User::name) {
                rule("must not be blank") { it.isNotBlank() }
            }
            validateEach(User::tags) {
                validate(Tag::value) {
                    rule("must not be blank") { it.isNotBlank() }
                }
            }
        }

        val result = validator.validate(User(name = "", tags = listOf(Tag(""), Tag("ok"))))
        assertEquals(2, result.errors.size)
        assertEquals("name", result.errors[0].path)
        assertEquals("tags[0].value", result.errors[1].path)
    }

}
