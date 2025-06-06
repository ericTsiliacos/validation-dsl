package validation.dsl

import org.testng.annotations.Test
import validation.core.*
import validation.dsl.scopes.FieldValidationScope
import validation.dsl.scopes.rule
import validation.dsl.grouping.group

class FieldValidationScopeTest {

    data class Profile(val name: String?, val tags: List<Tag>)
    data class Tag(val value: String)

    @Test
    fun `rule evaluates and reports errors`() {
        val result = fieldScope(PropertyPath("name"), Profile(null, emptyList())) {
            rule("Name must not be null") { it.name != null }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("name", "Name must not be null")
        }
    }

    @Test
    fun `rule accepts Rule and validates correctly`() {
        val notBlank = fromPredicate<String>(
            PropertyPath("username"), "must not be blank"
        ) { it.isNotBlank() }

        val result = fieldScope(PropertyPath("username"), "") {
            rule(notBlank)
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("username", "must not be blank")
        }
    }

    @Test
    fun `validate runs nested field validator`() {
        val result = fieldScope(PropertyPath("profile"), Profile("", emptyList())) {
            validate(Profile::name) {
                rule("must not be blank") { !it.isNullOrBlank() }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("profile.name", "must not be blank")
        }
    }

    @Test
    fun `validateEach runs validation for list items`() {
        val result = fieldScope(
            PropertyPath("profile"), Profile("ok", listOf(Tag(""), Tag("ok"), Tag(" ")))
        ) {
            validateEach(Profile::tags) {
                validate(Tag::value) {
                    rule("must not be blank") { it.isNotBlank() }
                }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("profile.tags[0].value", "must not be blank")
            errors[1].assertMatches("profile.tags[2].value", "must not be blank")
        }
    }

    @Test
    fun `FieldValidationScope should apply rules with and by default`() {
        val result = fieldScope(PropertyPath("field"), "BAD") {
            rule("must be lowercase") { it == it.lowercase() }
            rule("must be longer than 5") { it.length > 5 }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("field", "must be lowercase")
            errors[1].assertMatches("field", "must be longer than 5")
        }
    }

    @Test
    fun `group accumulates nested rule errors`() {
        val result = fieldScope(PropertyPath("field"), "") {
            group("basic rules") {
                rule("must not be blank") { it.isNotBlank() }
                rule("must be lowercase") { it == it.lowercase() }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("field", "must not be blank")
        }
    }

    @Test
    fun `group can wrap nested field validations`() {
        val profile = Profile("bob", listOf(Tag(""), Tag("ok")))
        val result = fieldScope(PropertyPath("profile"), profile) {
            group("tag validation") {
                validateEach(Profile::tags) {
                    validate(Tag::value) {
                        rule("must not be blank") { it.isNotBlank() }
                    }
                }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("profile.tags[0].value", "must not be blank")
        }
    }

}
