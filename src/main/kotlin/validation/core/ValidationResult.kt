package validation.core

data class ValidationError(
    val path: String,
    val message: String,
    val code: String? = null,
    val group: String? = null,
)

class ValidationResult(val errors: List<ValidationError>) {

    val isValid: Boolean get() = errors.isEmpty()

    companion object {
        fun from(validated: Validated<Unit>): ValidationResult = when (validated) {
            is Validated.Valid -> ValidationResult(emptyList())
            is Validated.Invalid -> ValidationResult(validated.errors)
        }

        fun fromMany(validatedList: List<Validated<Unit>>): ValidationResult =
            validatedList
                .filterIsInstance<Validated.Invalid>()
                .flatMap { it.errors }
                .let(::ValidationResult)
    }

}
