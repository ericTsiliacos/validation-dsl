package validation.core

import org.testng.annotations.Test

class RuleTest {

    @Test
    fun `fromPredicate returns Valid when predicate passes`() {
        val rule = fromPredicate<String>("field", "must not be blank") { it.isNotBlank() }
        rule("abc").assertValid()
    }

    @Test
    fun `fromPredicate returns Invalid when predicate fails`() {
        val rule = fromPredicate<String>("field", "must not be blank") { it.isNotBlank() }
        rule("").assertInvalid { errors ->
            errors[0].assertMatches("field", "must not be blank")
        }
    }

    @Test
    fun `fromPredicate includes code when provided`() {
        val rule = fromPredicate<String>(
            path = "field",
            message = "must not be blank",
            code = "error.blank"
        ) { it.isNotBlank() }

        rule("").assertInvalid { errors ->
            errors[0].assertMatches("field", "must not be blank", code = "error.blank")
        }
    }

}
