package validation.core

data class ValidationError(
    val path: String,
    val message: String,
    val code: String? = null,
    val group: String? = null,
)
