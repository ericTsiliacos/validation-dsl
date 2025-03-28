import org.testng.AssertJUnit.assertEquals
import org.testng.AssertJUnit.assertTrue
import org.testng.annotations.Test

class ValidatorTest {
    data class User(val name: String?, val email: String = "")

    @Test
    fun `name must not be blank`() {
        val validator = validator {
            validate(User::name) {
                rule("Name must not be blank") { !it.isNullOrBlank() }
            }
        }

        val result = validator.validate(User(name = "", email = ""))
        assertEquals(1, result.errors.size)
        assertEquals("name", result.errors[0].path)
        assertEquals("Name must not be blank", result.errors[0].message)
    }

    @Test
    fun `multiple rules on same field`() {
        val validator = validator {
            validate(User::name) {
                rule("Name must not be blank") { !it.isNullOrBlank() }
                rule("Name must be at least 3 characters") { it != null && it.length >= 3 }
            }
        }

        val result = validator.validate(User(name = "A", email = ""))
        assertEquals(1, result.errors.size)
        assertEquals("name", result.errors[0].path)
        assertEquals("Name must be at least 3 characters", result.errors[0].message)
    }

    @Test
    fun `rules across multiple fields`() {
        val validator = validator {
            validate(User::name) {
                rule("Name must not be blank") { !it.isNullOrBlank() }
            }
            validate(User::email) {
                rule("Email must not be blank") { it.isNotBlank() }
                rule("Email must contain @") { "@" in it }
            }
        }

        val result = validator.validate(User(name = null, email = ""))
        assertEquals(3, result.errors.size)

        assertEquals("name", result.errors[0].path)
        assertEquals("Name must not be blank", result.errors[0].message)

        assertEquals("email", result.errors[1].path)
        assertEquals("Email must not be blank", result.errors[1].message)

        assertEquals("email", result.errors[2].path)
        assertEquals("Email must contain @", result.errors[2].message)
    }

    @Test
    fun `chained rules short-circuit on first failure`() {
        val validator = validator {
            validate(User::name) {
                rule("must not be null") { it != null }
                    .andThen("must be at least 3 characters") { it!!.length >= 3 }
                    .andThen("must start with capital letter") { it?.get(0)?.isUpperCase() ?: false }
            }
        }

        val result = validator.validate(User(name = null))
        assertEquals(1, result.errors.size)
        assertEquals("name", result.errors[0].path)
        assertEquals("must not be null", result.errors[0].message)
    }

    @Test
    fun `chained rules continue if each passes`() {
        val validator = validator {
            validate(User::name) {
                rule("must not be null") { it != null }
                    .andThen("must be at least 3 characters") { it!!.length >= 3 }
                    .andThen("must start with capital letter") { it?.get(0)?.isUpperCase() ?: false }
            }
        }

        val result = validator.validate(User(name = "Al"))
        assertEquals(1, result.errors.size)
        assertEquals("must be at least 3 characters", result.errors[0].message)
    }

    @Test
    fun `all chained rules pass`() {
        val validator = validator {
            validate(User::name) {
                rule("must not be null") { it != null }
                    .andThen("must be at least 3 characters") { it!!.length >= 3 }
                    .andThen("must start with capital letter") { it?.get(0)?.isUpperCase() ?: false }
            }
        }

        val result = validator.validate(User(name = "Alex"))
        assertTrue(result.isValid)
    }

    @Test
    fun `validate nested object field`() {
        data class Address(val city: String?)
        data class User(val address: Address)

        val validator = validator {
            validate(User::address) {
                validate(Address::city) {
                    rule("City must not be null") { it != null }
                }
            }
        }

        val result = validator.validate(User(address = Address(city = null)))

        assertEquals(1, result.errors.size)
        assertEquals("address.city", result.errors[0].path)
        assertEquals("City must not be null", result.errors[0].message)
    }

    @Test
    fun `validateEach inside nested validate works`() {
        data class Child(val value: String)
        data class Parent(val children: List<Child>)

        val validator = validator {
            validateEach(Parent::children) {
                validate(Child::value) {
                    rule("Value must not be blank") { it.isNotBlank() }
                }
            }
        }

        val result = validator.validate(Parent(children = listOf(
            Child(""),
            Child("ok"),
            Child(" ")
        )))

        assertEquals(2, result.errors.size)
        assertEquals("children[0].value", result.errors[0].path)
        assertEquals("children[2].value", result.errors[1].path)
    }

    @Test
    fun `validateEach inside nested object field`() {
        data class Child(val value: String)
        data class Container(val children: List<Child>)
        data class Parent(val container: Container)

        val validator = validator {
            validate(Parent::container) {
                validateEach(Container::children) {
                    validate(Child::value) {
                        rule("Value must not be blank") { it.isNotBlank() }
                    }
                }
            }
        }

        val result = validator.validate(
            Parent(container = Container(
                children = listOf(Child(""), Child("ok"), Child(" "))
            ))
        )

        assertEquals(2, result.errors.size)
        assertEquals("container.children[0].value", result.errors[0].path)
        assertEquals("container.children[2].value", result.errors[1].path)
    }

    @Test
    fun `nullable field required and validated when present`() {
        data class User(val nickname: String?)

        val validator = validator {
            validate(User::nickname) {
                rule("Nickname must not be null") { it != null }

                whenNotNull{
                    rule("Nickname must be at least 3 characters") { it.length >= 3 }
                    rule("Nickname must only contain letters") { it.all { c -> c.isLetter() } }
                }
            }
        }

        val result = validator.validate(User(nickname = "ab"))
        assertEquals(1, result.errors.size)
        assertEquals("nickname", result.errors[0].path)
        assertEquals("Nickname must be at least 3 characters", result.errors[0].message)

        val invalidCharResult = validator.validate(User(nickname = "abc123"))
        assertEquals(1, invalidCharResult.errors.size)
        assertEquals("Nickname must only contain letters", invalidCharResult.errors[0].message)

        val validResult = validator.validate(User(nickname = "Abigail"))
        assertEquals(true, validResult.isValid)
    }

}
