package validation

data class ValidationError(val path: String, val message: String)

class ValidationResult(val errors: List<ValidationError>) {

    val isValid: Boolean get() = errors.isEmpty()

    companion object {
        fun from(validated: Validated<Unit>): ValidationResult = when (validated) {
            is Validated.Valid -> ValidationResult(emptyList())
            is Validated.Invalid -> ValidationResult(validated.errors)
        }

        fun fromMany(validatedList: List<Validated<Unit>>): ValidationResult {
            val allErrors = validatedList.filterIsInstance<Validated.Invalid>().flatMap { it.errors }
            return ValidationResult(allErrors)
        }
    }

}
