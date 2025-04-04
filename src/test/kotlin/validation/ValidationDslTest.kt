package validation

import org.testng.AssertJUnit.assertEquals
import org.testng.annotations.Test

class ValidationDslTest {

    data class User(val name: String?, val tags: List<String>)
    data class Address(val street: String, val city: String)
    data class Customer(val address: Address)

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
    fun `whenNotNull inside dependent is skipped if value is null`() {
        val result = fieldScope("field", null as String?) {
            dependent {
                this@fieldScope.whenNotNull {
                    rule("must be lowercase") { it == it.lowercase() }
                }
            }
        }

        result.assertValid()
    }

    @Test
    fun `dependent inside whenNotNull short-circuits if first rule fails`() {
        val result = fieldScope("field", "ABC" as String?) {
            whenNotNull {
                dependent {
                    rule("must be numeric") { it.all(Char::isDigit) }
                    rule("must be ≥ 3 digits") { it.length >= 3 }
                }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("field", "must be numeric")
        }
    }

    @Test
    fun `nested dependent blocks short-circuit inner but not outer`() {
        val result = fieldScope("value", "abc") {
            dependent {
                rule("must be lowercase") { it == it.lowercase() }

                this@fieldScope.dependent {
                    rule("must be numeric") { it.all(Char::isDigit) }
                    rule("must be 3 chars") { it.length == 3 }
                }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("value", "must be numeric")
        }
    }

    @Test
    fun `validateEach validates each item in list`() {
        val result = fieldScope("tags", listOf("", "ok", " ")) {
            validateEach {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("tags[0]", "must not be blank")
            errors[1].assertMatches("tags[2]", "must not be blank")
        }
    }

    @Test
    fun `validateEach handles empty list as valid`() {
        val result = fieldScope("tags", emptyList<String>()) {
            validateEach {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        result.assertValid()
    }

    @Test
    fun `multiple rules for same field accumulate errors`() {
        val result = fieldScope("username", "ABC") {
            rule("must not be blank") { it.isNotBlank() }
            rule("must be lowercase") { it == it.lowercase() }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("username", "must be lowercase")
        }
    }

    @Test
    fun `nested validateEach handles deeply nested lists`() {
        data class Item(val tags: List<String>)
        data class Order(val items: List<Item>)

        val validator = validator {
            validateEach(Order::items) {
                validateEach(Item::tags) {
                    rule("must not be blank") { it.isNotBlank() }
                }
            }
        }

        val result = validator.validate(
            Order(
                listOf(
                    Item(listOf("ok", "")),
                    Item(listOf(" ", "ok"))
                )
            )
        )

        result.assertInvalid { errors ->
            errors[0].assertMatches("items[0].tags[1]", "must not be blank")
            errors[1].assertMatches("items[1].tags[0]", "must not be blank")
        }
    }

    @Test
    fun `ValidationResult fromMany handles empty list as valid`() {
        val result = ValidationResult.fromMany(emptyList())
        result.assertValid()
    }

    @Test
    fun `ValidationError supports multiple codes for same path`() {
        val result = Validated.Invalid(
            listOf(
                ValidationError("field", "too short", code = "short"),
                ValidationError("field", "must be lowercase", code = "lowercase")
            )
        )

        result.assertInvalid { errors ->
            errors[0].assertMatches("field", "too short", code = "short")
            errors[1].assertMatches("field", "must be lowercase", code = "lowercase")
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

    @Test
    fun `group errors include label in metadata`() {
        val validator = validator {
            validate(User::name) {
                group("name group") {
                    rule("must not be blank") { !it.isNullOrBlank() }
                    rule("must be longer than 3") { it != null && it.length > 3 }
                }
            }
        }

        val result = validator.validate(User("", listOf()))

        result.assertInvalid { errors ->
            val groups = errors.map { it.group }
            assert(groups.contains("name group"))
            assert(groups.count { it == "name group" } == 2)
        }
    }

    @Test
    fun `group in DSL validator accumulates rule errors`() {
        val validator = validator {
            validate(User::name) {
                group("name checks") {
                    rule("must not be null") { it != null }
                    rule("must be longer than 2 characters") { it != null && it.length > 2 }
                }
            }
        }

        val result = validator.validate(User(null, listOf()))

        result.assertInvalid { errors ->
            errors[0].assertMatches("name", "must not be null")
        }
    }

    @Test
    fun `group in DSL validator wraps nested validations`() {
        val validator = validator {
            validate(Customer::address) {
                group("address rules") {
                    validate(Address::street) {
                        rule("must not be blank") { it.isNotBlank() }
                    }
                    validate(Address::city) {
                        rule("must not be blank") { it.isNotBlank() }
                    }
                }
            }
        }

        val result = validator.validate(Customer(Address("", "")))

        result.assertInvalid { errors ->
            errors[0].assertMatches(
                path = "address.street",
                message = "must not be blank",
                group = "address rules",
            )
            errors[1].assertMatches(
                path = "address.city",
                message = "must not be blank",
                group = "address rules",
            )
        }
    }

    @Test
    fun `andThen short-circuits if first rule fails`() {
        val rule1 = fromPredicate<String>("x", "must not be empty") { it.isNotEmpty() }
        val rule2 = fromPredicate<String>("x", "must be lowercase") { it == it.lowercase() }

        val combined = rule1 andThen rule2

        combined("").assertInvalid { errors ->
            errors[0].assertMatches("x", "must not be empty")
        }
    }

    @Test
    fun `andThen runs second rule only if first passes`() {
        val rule1 = fromPredicate<String>("x", "must not be empty") { it.isNotEmpty() }
        val rule2 = fromPredicate<String>("x", "must be lowercase") { it == it.lowercase() }

        val combined = rule1 andThen rule2

        combined("Hello").assertInvalid { errors ->
            errors[0].assertMatches("x", "must be lowercase")
        }
    }

    @Test
    fun `combine accumulates both errors`() {
        val rule1 = fromPredicate<String>("x", "must be longer than 3") { it.length > 3 }
        val rule2 = fromPredicate<String>("x", "must be lowercase") { it == it.lowercase() }

        val combined = rule1 combine rule2

        combined("Hello").assertInvalid { errors ->
            assertEquals(1, errors.size)
            errors[0].assertMatches("x", "must be lowercase")
        }

        combined("A").assertInvalid { errors ->
            errors[0].assertMatches("x", "must be longer than 3")
            errors[1].assertMatches("x", "must be lowercase")
        }
    }

    @Test
    fun `predicate assigns path automatically when used in validation scope`() {
        val notBlank = predicate<String>("must not be blank") { it.isNotBlank() }

        val result = fieldScope("username", "") {
            rule(notBlank)
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("username", "must not be blank")
        }
    }

    @Test
    fun `predicate passes validation when condition is met`() {
        val atLeast3 = predicate<String>("must be at least 3 characters") { it.length >= 3 }

        val result = fieldScope("name", "Bob") {
            rule(atLeast3)
        }

        result.assertValid()
    }

    @Test
    fun `predicate supports optional code`() {
        val coded = predicate<String>("must be numeric", code = "numeric") { it.all(Char::isDigit) }

        val result = fieldScope("age", "abc") {
            rule(coded)
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("age", "must be numeric", code = "numeric")
        }
    }

    @Test
    fun `fromFunction returns valid when rule succeeds`() {
        val rule = fromFunction<String> {
            Validated.Valid(Unit)
        }
        rule("any input").assertValid()
    }

    @Test
    fun `fromFunction handles null safely if rule allows`() {
        val rule = fromFunction<String?> {
            if (it == null) Validated.Invalid(listOf(ValidationError("field", "must not be null")))
            else Validated.Valid(Unit)
        }

        rule(null).assertInvalid { errors ->
            errors[0].assertMatches("field", "must not be null")
        }
    }

    @Test
    fun `fromFunction supports reusable rule objects`() {
        val reusableRule: Rule<String> = {
            if (it.startsWith("a")) Validated.Valid(Unit)
            else Validated.Invalid(listOf(ValidationError("value", "must start with 'a'")))
        }

        val rule1 = fromFunction(reusableRule)
        val rule2 = fromFunction(reusableRule)

        rule1("abc").assertValid()
        rule2("xyz").assertInvalid { errors ->
            errors[0].assertMatches("value", "must start with 'a'")
        }
    }

    @Test
    fun `fromFunction wraps raw rule correctly`() {
        val rawRule: Rule<String> = { input ->
            if (input.length > 3) Validated.Valid(Unit)
            else Validated.Invalid(listOf(ValidationError("field", "too short")))
        }

        val rule = fromFunction(rawRule)

        rule("okay").assertValid()
        rule("no").assertInvalid { errors ->
            errors[0].assertMatches("field", "too short")
        }
    }

}
