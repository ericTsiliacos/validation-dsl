package validation.core

import org.testng.annotations.Test
import validation.rules.andThen

class RuleBuilderTest {

    @Test
    fun `rule from predicate passes and fails correctly`() {
        val rule: Rule<String> = fromPredicate(PropertyPath("username"), "must not be blank") {
            it.isNotBlank()
        }

        rule("hello").assertValid()

        rule("").assertInvalid { errors ->
            errors[0].assertMatches(PropertyPath("username"), "must not be blank")
        }
    }

    @Test
    fun `andThen chains dependent rules and short-circuits on failure`() {
        val rule1 = fromPredicate<String>(
            PropertyPath("age"), "must be numeric"
        ) { it.all { c -> c.isDigit() } }

        val rule2 = fromPredicate<String>(
            PropertyPath("age"), "must be >= 18"
        ) { it.toInt() >= 18 }

        val chained = rule1 andThen rule2

        chained("21").assertValid()

        chained("abc").assertInvalid { errors ->
            errors[0].assertMatches("age", "must be numeric")
        }

        chained("17").assertInvalid { errors ->
            errors[0].assertMatches("age", "must be >= 18")
        }
    }


}
