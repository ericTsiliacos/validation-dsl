package validation

import org.testng.annotations.Test

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

}
