package validation.core

data class ValidationError internal constructor(
    val path: PropertyPath = PropertyPath.EMPTY,
    val message: String,
    val code: String? = null,
    val group: String? = null,
) {

    companion object {
        fun root(message: String, code: String? = null, group: String? = null): ValidationError =
            ValidationError(PropertyPath.root(), message, code, group)
    }

}
