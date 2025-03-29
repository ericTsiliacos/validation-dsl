package validation

import org.testng.AssertJUnit.assertEquals
import org.testng.AssertJUnit.assertTrue
import org.testng.annotations.Test
import validation.RuleBuilder.ValidationNode.PredicateRule

class FieldValidationScopeTest {
    data class Profile(val name: String?, val tags: List<Tag>)
    data class Tag(val value: String)

    @Test
    fun `rule evaluates and reports errors`() {
        val scope = FieldValidationScope("name") { Profile(null, emptyList()) }
        scope.rule("Name must not be null") { it.name != null }

        val errors = scope.evaluate()
        assertEquals(1, errors.size)
        assertEquals("name", errors[0].path)
        assertEquals("Name must not be null", errors[0].message)
    }

    @Test
    fun `rule accepts PredicateRule and validates correctly`() {
        val notBlank = PredicateRule<String>("must not be blank") { it.isNotBlank() }

        val scope = FieldValidationScope("username") { "" }
        scope.rule(notBlank)

        val errors = scope.evaluate()

        assertEquals(1, errors.size)
        assertEquals("username", errors[0].path)
        assertEquals("must not be blank", errors[0].message)
    }

    @Test
    fun `validate runs nested field validator`() {
        val scope = FieldValidationScope("profile") { Profile("", emptyList()) }
        scope.validate(Profile::name) {
            rule("must not be blank") { !it.isNullOrBlank() }
        }

        val errors = scope.evaluate()
        assertEquals("profile.name", errors[0].path)
    }

    @Test
    fun `validateEach runs validation for list items`() {
        val scope = FieldValidationScope("profile") {
            Profile("ok", listOf(Tag(""), Tag("ok"), Tag(" ")))
        }

        scope.validateEach(Profile::tags) {
            validate(Tag::value) {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        val errors = scope.evaluate()
        assertEquals(2, errors.size)
        assertEquals("profile.tags[0].value", errors[0].path)
        assertEquals("profile.tags[2].value", errors[1].path)
    }

    @Test
    fun `whenNotNull block is only evaluated when value is not null`() {
        data class User(val nickname: String?)

        val validator = validator<User> {
            validate(User::nickname) {
                whenNotNull {
                    rule("must be at least 3 characters") { it.length >= 3 }
                }
            }
        }

        val nullResult = validator.validate(User(nickname = null))
        assertTrue(nullResult.isValid)

        val shortResult = validator.validate(User(nickname = "ab"))
        assertEquals(1, shortResult.errors.size)
        assertEquals("must be at least 3 characters", shortResult.errors[0].message)

        val validResult = validator.validate(User(nickname = "abcd"))
        assertTrue(validResult.isValid)
    }

    @Test
    fun `andThen builds dependent rule chain`() {
        val scope = FieldValidationScope("age") { "18" }

        scope.rule("must be numeric") { it.all { c -> c.isDigit() } }
            .andThen("must be at least 18") { it.toInt() >= 18 }

        val result = scope.evaluate()
        assertTrue(result.isEmpty())
    }

}
