package validation

import org.testng.AssertJUnit.*
import org.testng.annotations.Test

class ValidateEachUtilTest {

    data class Item(val value: String)

    @Test
    fun `validateEachList applies block to each item with indexed path`() {
        val items = listOf(Item(""), Item("ok"), Item(" "))

        val errors = validateEachList(items, "tags") {
            rule("must not be blank") { it.value.isNotBlank() }
        }

        assertEquals(2, errors.size)

        assertEquals("tags[0]", errors[0].path)
        assertEquals("must not be blank", errors[0].message)

        assertEquals("tags[2]", errors[1].path)
        assertEquals("must not be blank", errors[1].message)
    }

}
