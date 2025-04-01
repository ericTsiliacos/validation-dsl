import main.kotlin.validation.validator

fun main() {
    data class Tag(val value: String)
    data class User(val tags: List<Tag>)

    val result = validator {
        validateEach(User::tags) {
            validate(Tag::value) {
                rule("Tag value must not be blank") { it.isNotBlank() }
            }
        }
    }.validate(User(tags = listOf(Tag(""), Tag("hello"), Tag(" "))))

    println(result.errors)
}
