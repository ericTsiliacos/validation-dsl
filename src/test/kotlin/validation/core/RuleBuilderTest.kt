package validation.core

import org.testng.annotations.Test
import validation.extensions.andThen
import validation.extensions.combine

class RuleBuilderTest {

    @Test
    fun `rule from predicate passes and fails correctly`() {
        val rule: Rule<String> = fromPredicate("username", "must not be blank") {
            it.isNotBlank()
        }

        rule("hello").assertValid()

        rule("").assertInvalid { errors ->
            errors[0].assertMatches("username", "must not be blank")
        }
    }

    @Test
    fun `andThen chains dependent rules and short-circuits on failure`() {
        val rule1 = fromPredicate<String>("age", "must be numeric") { it.all { c -> c.isDigit() } }
        val rule2 = fromPredicate<String>("age", "must be >= 18") { it.toInt() >= 18 }
        val chained = rule1 andThen rule2

        chained("21").assertValid()

        chained("abc").assertInvalid { errors ->
            errors[0].assertMatches("age", "must be numeric")
        }

        chained("17").assertInvalid { errors ->
            errors[0].assertMatches("age", "must be >= 18")
        }
    }

    @Test
    fun `combine applies both rules and accumulates errors`() {
        val r1 = fromPredicate<String>("username", "must be longer than 3") { it.length > 3 }
        val r2 = fromPredicate<String>("username", "must be lowercase") { it == it.lowercase() }

        val combined = r1 combine r2

        combined("A").assertInvalid { errors ->
            errors[0].assertMatches("username", "must be longer than 3")
            errors[1].assertMatches("username", "must be lowercase")
        }
    }

    @Test
    fun `build returns composed rule`() {
        val builder = RuleBuilder(fromPredicate<String>("x", "must be number") { it.all(Char::isDigit) })
            .andThen("must be 3 chars") { it.length == 3 }

        val built = builder.build()
        built("123").assertValid()
    }

}
