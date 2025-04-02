package validation

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
        result.assertInvalid { errors ->
            errors[0].assertMatches("name", "must not be null")
        }
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
        result.assertValid()
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
        result.assertInvalid { errors ->
            errors[0].assertMatches("name", "must be at least 3 chars")
        }
    }

    @Test
    fun `validateEachItem validates each item in list`() {
        val result = fieldScope("tags", listOf("", "ok", " ")) {
            validateEachItem {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("tags[0]", "must not be blank")
            errors[1].assertMatches("tags[2]", "must not be blank")
        }
    }

    @Test
    fun `dependent rules short-circuit on first failure`() {
        val result = fieldScope("age", "abc") {
            dependent {
                rule("must be numeric") { it.all(Char::isDigit) }
                rule("must be ≥ 18") { it.toInt() >= 18 }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("age", "must be numeric")
        }
    }

    @Test
    fun `dependent rules continue if first passes`() {
        val result = fieldScope("age", "15") {
            dependent {
                rule("must be numeric") { it.all(Char::isDigit) }
                rule("must be ≥ 18") { it.toInt() >= 18 }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("age", "must be ≥ 18")
        }
    }

    @Test
    fun `dependent block with one rule works`() {
        val result = fieldScope("username", "") {
            dependent {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("username", "must not be blank")
        }
    }

    @Test
    fun `dependent block with zero rules does nothing`() {
        val result = fieldScope("noop", "ok") {
            dependent {}
        }

        result.assertValid()
    }

}
