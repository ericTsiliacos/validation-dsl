package validation.core

import org.testng.AssertJUnit.assertEquals
import org.testng.annotations.Test

class PropertyPathTest {

    @Test
    fun `toString returns single segment correctly`() {
        val path = PropertyPath("name")
        assertEquals("name", path.toString())
    }

    @Test
    fun `toString returns dot notation for nested paths`() {
        val path = PropertyPath("user").child("email")
        assertEquals("user.email", path.toString())
    }

    @Test
    fun `toString formats indexed paths correctly`() {
        val path = PropertyPath("items").index(0).child("name")
        assertEquals("items[0].name", path.toString())
    }

    @Test
    fun `empty path renders to empty string`() {
        val path = PropertyPath.EMPTY
        assertEquals("", path.toString())
    }

    @Test
    fun `index adds bracketed segment`() {
        val path = PropertyPath("tags").index(2)
        assertEquals("tags[2]", path.toString())
    }

    @Test
    fun `deep nesting with indexes and fields`() {
        val path = PropertyPath("orders").index(1).child("items").index(3).child("name")
        assertEquals("orders[1].items[3].name", path.toString())
    }

    @Test
    fun `PropertyPath equality works correctly`() {
        val a = PropertyPath("user").child("email")
        val b = PropertyPath("user").child("email")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `PropertyPath inequality works correctly`() {
        val a = PropertyPath("user").child("email")
        val b = PropertyPath("user").child("name")
        assert(a != b)
    }

    @Test
    fun `PropertyPath with index segments are not equal to simple children`() {
        val a = PropertyPath("items").index(0)
        val b = PropertyPath("items").child("0")
        assert(a != b)
    }

}