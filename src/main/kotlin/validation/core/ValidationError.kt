package validation.core

data class ValidationError(
    val path: PropertyPath = PropertyPath.EMPTY,
    val message: String,
    val code: String? = null,
    val group: String? = null,
)
