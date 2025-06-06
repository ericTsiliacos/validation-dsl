package validation.dsl

import org.testng.AssertJUnit.assertEquals
import org.testng.AssertJUnit.assertTrue
import org.testng.annotations.Test
import validation.core.*
import validation.dsl.builder.Validator
import validation.dsl.builder.validator
import validation.dsl.scopes.FieldValidationScope
import validation.dsl.scopes.rule
import validation.dsl.scopes.predicate
import validation.dsl.grouping.group
import validation.dsl.nullable.whenNotNull
import validation.dsl.nullable.ruleIfPresent
import validation.dsl.collection.validateEach

class ValidationDslTest {

    data class User(val name: String?, val tags: List<String>)
    data class Address(val street: String, val city: String)
    data class Customer(val address: Address)

    private fun <T> fieldScope(path: String, value: T, block: FieldValidationScope<T>.() -> Unit): Validated<Unit> {
        return FieldValidationScope(PropertyPath(path)) { value }.apply(block).evaluate()
    }

    @Test
    fun `predicate returns Valid when condition passes`() {
        val isPositive = predicate<Int>("must be positive") { it > 0 }
        val result = isPositive(5)
        assertTrue(result is Validated.Valid)
    }

    @Test
    fun `predicate returns Invalid when condition fails`() {
        val isPositive = predicate<Int>("must be positive") { it > 0 }
        val result = isPositive(-1)
        assertTrue(result is Validated.Invalid)
        result as Validated.Invalid
        assertEquals("must be positive", result.errors.first().message)
    }

    @Test
    fun `rule injects path into predicate error`() {
        val notBlank = predicate<String>("must not be blank") { it.isNotBlank() }
        val result = fieldScope("username", "") {
            rule(notBlank)
        }

        assertTrue(result is Validated.Invalid)
        result as Validated.Invalid
        val error = result.errors.first()
        assertEquals("username", error.path.toString())
        assertEquals("must not be blank", error.message)
    }

    @Test
    fun `rule with inline predicate works correctly`() {
        val result = fieldScope("username", "") {
            rule("must not be blank") { it.isNotBlank() }
        }

        assertTrue(result is Validated.Invalid)
        result as Validated.Invalid
        val error = result.errors.first()
        assertEquals("username", error.path.toString())
        assertEquals("must not be blank", error.message)
    }

    @Test
    fun `rule with message, code, and group is preserved`() {
        val result = fieldScope("field", "") {
            rule(
                message = "must not be blank",
                code = "BLANK",
                group = "basic"
            ) { it.isNotBlank() }
        }

        result.assertInvalid { errors ->
            val error = errors.first()
            error.assertMatches("field", "must not be blank", code = "BLANK", group = "basic")
        }
    }

    @Test
    fun `top-level rule uses root path and preserves metadata`() {
        val rule = rule<String>(
            message = "forbidden value",
            code = "FORBID",
            group = "security"
        ) { it == "unblocked" }

        val result = rule("blocked")
        assertTrue(result is Validated.Invalid)
        result as Validated.Invalid
        val error = result.errors.first()
        error.assertMatches("", "forbidden value", code = "FORBID", group = "security")
    }

    @Test
    fun `group passes through when no errors occur`() {
        val result = fieldScope("field", "valid") {
            group("label") {
                rule("must be non-empty") { it.isNotEmpty() }
            }
        }

        result.assertValid()
    }

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
    fun `ruleIfPresent skips rule when value is null`() {
        val validator = validator {
            validate(User::name) {
                ruleIfPresent("must be at least 3 chars") { it.length >= 3 }
            }
        }

        val result = validator.validate(User(null, listOf()))
        result.assertValid()
    }

    @Test
    fun `ruleIfPresent applies rule when value is not null`() {
        val validator = validator {
            validate(User::name) {
                ruleIfPresent("must be at least 3 chars") { it.length >= 3 }
            }
        }

        val result = validator.validate(User("ab", listOf()))
        result.assertInvalid { errors ->
            errors[0].assertMatches("name", "must be at least 3 chars")
        }
    }

    @Test
    fun `dependent inside whenNotNull short-circuits if first rule fails`() {
        val result = fieldScope("field", "ABC" as String?) {
            whenNotNull {
                dependent {
                    rule("must be numeric") { it.all(Char::isDigit) }
                    rule("must be greater than or equal to 3 digits") { it.length >= 3 }
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
    fun `validateEach works on List root`() {
        val result = fieldScope(PropertyPath("tags"), listOf("", "ok", " ")) {
            validateEach {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches(PropertyPath("tags").index(0), "must not be blank")
            errors[1].assertMatches(PropertyPath("tags").index(2), "must not be blank")
        }
    }

    @Test
    fun `validateEach with empty list is valid`() {
        val result = fieldScope(PropertyPath("tags"), emptyList<String>()) {
            validateEach {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        result.assertValid()
    }

    @Test
    fun `validateEach handles null items safely`() {
        val result = fieldScope(PropertyPath("tags"), listOf(null, "ok", null)) {
            validateEach {
                whenNotNull {
                    rule("must not be blank") { it.isNotBlank() }
                }
            }
        }

        result.assertValid()
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
                ValidationError(PropertyPath("field"), "too short", code = "short"),
                ValidationError(PropertyPath("field"), "must be lowercase", code = "lowercase")
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
                rule("must be greater than or equal to 18") { it.toInt() >= 18 }
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
                rule("must be greater than or equal to 18") { it.toInt() >= 18 }
            }
        }

        result.assertInvalid { errors ->
            errors[0].assertMatches("age", "must be greater than or equal to 18")
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
    fun `use should return valid when nested validator passes`() {
        data class Profile(val bio: String)
        data class User(val profile: Profile)

        val profileValidator = validator {
            validate(Profile::bio) {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        val userValidator = validator {
            validate(User::profile) {
                use(profileValidator)
            }
        }

        val result = userValidator.validate(User(Profile("non-blank")))
        assertTrue(result.isValid())
    }

    @Test
    fun `use should return errors with nested path when validator fails`() {
        data class Profile(val bio: String)
        data class User(val profile: Profile)

        val profileValidator = validator {
            validate(Profile::bio) {
                rule("must not be blank") { it.isBlank() } // fail deliberately
            }
        }

        val userValidator = validator {
            validate(User::profile) {
                use(profileValidator)
            }
        }

        val result = userValidator.validate(User(Profile("valid")))
        result.assertError("profile.bio", "must not be blank")
    }

    @Test
    fun `use should work with whenNotNull for nullable fields`() {
        data class Profile(val bio: String)
        data class User(val profile: Profile?)

        val profileValidator = validator {
            validate(Profile::bio) {
                rule("must not be blank") { it.isBlank() }
            }
        }

        val userValidator = validator {
            validate(User::profile) {
                whenNotNull {
                    use(profileValidator)
                }
            }
        }

        val result = userValidator.validate(User(null))
        assertTrue(result.isValid())
    }

    @Test
    fun `use should work inside validateEach for lists of objects`() {
        data class Address(val city: String)
        data class User(val addresses: List<Address>)

        val addressValidator = validator {
            validate(Address::city) {
                rule("must not be blank") { it.isNotBlank() }
            }
        }

        val userValidator = validator {
            validateEach(User::addresses) {
                use(addressValidator)
            }
        }

        val result = userValidator.validate(User(listOf(Address(""), Address("NY"))))
        result.assertError("addresses[0].city", "must not be blank")
    }

    @Test
    fun `use should preserve group label when used inside group block`() {
        data class Profile(val bio: String)
        data class User(val profile: Profile)

        val profileValidator = validator {
            validate(Profile::bio) {
                rule("must not be blank") { it.isBlank() }
            }
        }

        val userValidator = validator {
            validate(User::profile) {
                group("profile-checks") {
                    use(profileValidator)
                }
            }
        }

        val result = userValidator.validate(User(Profile("ok")))
        result.assertError("profile.bio", "must not be blank")

        val group = (result as ValidationResult.Invalid).errors.first().group
        assertEquals("profile-checks", group)
    }

    @Test
    fun `use should handle root path errors`() {
        data class Leaf(val value: String)
        data class Wrapper(val leaf: Leaf)

        val leafValidator = validator<Leaf> {
            root {
                rule("bad") { false }
            }
        }

        val wrapperValidator = validator {
            validate(Wrapper::leaf) {
                use(leafValidator)
            }
        }

        val result = wrapperValidator.validate(Wrapper(Leaf("x")))
        result.assertError("leaf", "bad")
    }

}
