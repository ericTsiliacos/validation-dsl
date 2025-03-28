import org.testng.AssertJUnit.assertEquals
import org.testng.AssertJUnit.assertTrue
import org.testng.annotations.Test

class ValidatorTest {

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
    fun `validate each item in a list field`() {
        data class Tag(val value: String)
        data class User(val tags: List<Tag>)

        val validator = validator {
            validateEach(User::tags) {
                validate(Tag::value) {
                    rule("Tag value must not be blank") { it.isNotBlank() }
                }
            }
        }

        val result = validator.validate(User(tags = listOf(Tag(""), Tag("hello"), Tag(" "))))

        assertEquals(2, result.errors.size)

        assertEquals("tags[0].value", result.errors[0].path)
        assertEquals("Tag value must not be blank", result.errors[0].message)

        assertEquals("tags[2].value", result.errors[1].path)
        assertEquals("Tag value must not be blank", result.errors[1].message)
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

}
