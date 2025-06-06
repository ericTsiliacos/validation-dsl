package validation.dsl

import org.testng.AssertJUnit.assertEquals
import org.testng.AssertJUnit.assertTrue
import org.testng.annotations.Test
import validation.core.*
import validation.dsl.builder.Validator
import validation.dsl.builder.validator
import validation.dsl.scopes.rule

class ValidatorTest {

    data class User(val name: String, val tags: List<Tag>)
    data class Tag(val value: String)

    @Test
    fun `validate registers field rules`() {
        val validator = Validator<User>()
        validator.validate(User::name) {
            rule("must not be blank") { it.isNotBlank() }
        }

        val validated = validator.validate(User(name = "", tags = emptyList())).toValidated()
        validated.assertInvalid { errors ->
            errors[0].assertMatches("name", "must not be blank")
        }
    }

    @Test
    fun `validateEach applies rules to list elements`() {
        val validator = Validator<User>()
        validator.validateEach(User::tags) {
            validate(Tag::value) {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        val validated = validator.validate(User(name = "ok", tags = listOf(Tag(""), Tag("x")))).toValidated()
        validated.assertInvalid { errors ->
            errors[0].assertMatches("tags[0].value", "must not be blank")
        }
    }

    @Test
    fun `validator function builds Validator instance`() {
        val validator = validator {
            validate(User::name) {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        val validated = validator.validate(User(name = "", tags = emptyList())).toValidated()
        validated.assertInvalid { errors ->
            errors[0].assertMatches("name", "must not be blank")
        }
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

        val validated = validator.validate(User(name = "", tags = listOf(Tag(""), Tag("ok")))).toValidated()
        validated.assertInvalid { errors ->
            errors[0].assertMatches("name", "must not be blank")
            errors[1].assertMatches("tags[0].value", "must not be blank")
        }
    }


    @Test
    fun `toUnit on Valid should return Valid of Unit`() {
        val validated: Validated<String> = Validated.Valid("hello")

        val result = validated.toUnit()

        assertTrue(result is Validated.Valid)
        assertEquals(Unit, (result as Validated.Valid).value)
    }

    @Test
    fun `toUnit on Invalid should return same Invalid`() {
        val error = ValidationError(PropertyPath("path"), "error message", "ERR")
        val validated: Validated<String> = Validated.Invalid(listOf(error))

        val result = validated.toUnit()

        assertTrue(result is Validated.Invalid)
        assertEquals(listOf(error), (result as Validated.Invalid).errors)
    }

}
